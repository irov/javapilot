package org.pilot.sdk;

import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Pilot SDK — remote debug panel and logging for Android applications.
 *
 * <p>Singleton entry point. Initialize once, then call methods from anywhere in your app.</p>
 *
 * <pre>{@code
 * // 1. Initialize
 * PilotConfig config = new PilotConfig.Builder("https://pilot.example.com", "plt_your_token")
 *     .setDeviceId("my-device")
 *     .setDeviceName("Pixel 8 (Android 14)")
 *     .build();
 * Pilot.initialize(config);
 *
 * // 2. Listen for actions
 * Pilot.addActionListener(action -> {
 *     if ("btn-restart".equals(action.getWidgetId())) {
 *         restartGame();
 *         Pilot.acknowledgeAction(action.getId(), null);
 *     }
 * });
 *
 * // 3. Build UI
 * PilotUI ui = Pilot.getUI();
 * PilotTab tab = ui.addTab("Controls");
 * PilotLayout root = tab.vertical();
 * root.addButton("Restart")
 *     .variant("contained").color("error")
 *     .onClick(action -> restartGame());
 *
 * // 4. Connect
 * Pilot.connect();
 *
 * // 5. Send logs anytime
 * Pilot.log(PilotLogLevel.INFO, "Game started");
 *
 * // 6. Shutdown on exit
 * Pilot.shutdown();
 * }</pre>
 */
public final class Pilot {
    public static final String VERSION = "1.0.13";

    private static volatile Pilot s_instance;

    private final PilotConfig m_config;
    private final PilotHttpClient m_httpClient;

    private final AtomicReference<String> m_sessionToken = new AtomicReference<>(null);
    private final AtomicReference<String> m_requestId = new AtomicReference<>(null);
    private final AtomicReference<PilotSessionStatus> m_status = new AtomicReference<>(PilotSessionStatus.DISCONNECTED);
    private final AtomicBoolean m_running = new AtomicBoolean(false);

    private final List<PilotLogEntry> m_logBuffer = Collections.synchronizedList(new ArrayList<>());
    private final AtomicBoolean m_logOverflowWarned = new AtomicBoolean(false);
    private final Map<String, String> m_sessionAttributeCache = new ConcurrentHashMap<>();

    private final CopyOnWriteArrayList<PilotActionListener> m_actionListeners = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<PilotSessionListener> m_sessionListeners = new CopyOnWriteArrayList<>();

    private ScheduledExecutorService m_executor;
    private ScheduledFuture<?> m_heartbeatFuture;
    private ScheduledFuture<?> m_actionPollFuture;
    private ScheduledFuture<?> m_logFlushFuture;

    private final PilotUI m_ui = new PilotUI();
    private final Handler m_mainHandler = new Handler(Looper.getMainLooper());

    private Pilot(@NonNull PilotConfig config) {
        m_config = config;
        m_httpClient = new PilotHttpClient(config.baseUrl, config.apiToken);
        PilotLog.setLevel(config.logLevel);
        PilotLog.setLogger(config.logger);
    }

    // ══════════════════════════════════════════════════════
    //  Static API
    // ══════════════════════════════════════════════════════

    /**
     * Initialize the Pilot SDK. Must be called before any other method.
     * Safe to call multiple times — subsequent calls are ignored.
     */
    public static void initialize(@NonNull PilotConfig config) {
        if (s_instance != null) {
            PilotLog.w("Pilot.initialize() called more than once, ignoring");
            return;
        }

        synchronized (Pilot.class) {
            if (s_instance == null) {
                Pilot p = new Pilot(config);
                s_instance = p;
                PilotLog.i("Pilot SDK initialized (server: %s)", config.baseUrl);

                if (config.sessionListener != null) {
                    p.m_sessionListeners.add(config.sessionListener);
                }
                if (config.actionListener != null) {
                    p.m_actionListeners.add(config.actionListener);
                }

                if (config.autoConnect) {
                    p.startConnection();
                }
            }
        }
    }

    /**
     * Whether the SDK has been initialized.
     */
    public static boolean isInitialized() {
        return s_instance != null;
    }

    /**
     * Current session status.
     */
    @NonNull
    public static PilotSessionStatus getStatus() {
        Pilot p = s_instance;
        if (p == null) {
            return PilotSessionStatus.DISCONNECTED;
        }
        return p.m_status.get();
    }

    /**
     * Add a listener for actions from the dashboard.
     */
    public static void addActionListener(@NonNull PilotActionListener listener) {
        Pilot p = requireInstance();
        p.m_actionListeners.add(listener);
    }

    /**
     * Remove a previously added action listener.
     */
    public static void removeActionListener(@NonNull PilotActionListener listener) {
        Pilot p = s_instance;
        if (p != null) {
            p.m_actionListeners.remove(listener);
        }
    }

    /**
     * Add a listener for session lifecycle events.
     */
    public static void addSessionListener(@NonNull PilotSessionListener listener) {
        Pilot p = requireInstance();
        p.m_sessionListeners.add(listener);
    }

    /**
     * Remove a previously added session listener.
     */
    public static void removeSessionListener(@NonNull PilotSessionListener listener) {
        Pilot p = s_instance;
        if (p != null) {
            p.m_sessionListeners.remove(listener);
        }
    }

    /**
     * Get the central UI data source. Each service/module adds its own tab here.
     * Changes are automatically sent to the server on the next poll cycle.
     *
     * @return The shared PilotUI instance
     */
    @NonNull
    public static PilotUI getUI() {
        return requireInstance().m_ui;
    }

    /**
     * Connect to the Pilot server. Runs asynchronously.
     */
    public static void connect() {
        Pilot p = requireInstance();
        p.startConnection();
    }

    /**
     * Disconnect from the server and stop all background tasks.
     */
    public static void disconnect() {
        Pilot p = s_instance;
        if (p != null) {
            p.stopConnection();
        }
    }

    /**
     * Send a log entry to the Pilot server. Logs are buffered and sent in batches.
     * Log attributes are resolved at call time.
     */
    public static void log(@NonNull PilotLogLevel level, @NonNull String message) {
        Pilot p = s_instance;
        if (p != null) {
            p.bufferLog(new PilotLogEntry(level, message, null, null, null, p.resolveLogAttributes()));
        }
    }

    /**
     * Send a log entry with category and thread.
     */
    public static void log(@NonNull PilotLogLevel level, @NonNull String message,
                           @Nullable String category, @Nullable String thread) {
        Pilot p = s_instance;
        if (p != null) {
            p.bufferLog(new PilotLogEntry(level, message, category, thread, null, p.resolveLogAttributes()));
        }
    }

    /**
     * Send a log entry with metadata.
     */
    public static void log(@NonNull PilotLogLevel level, @NonNull String message, @Nullable JSONObject metadata) {
        Pilot p = s_instance;
        if (p != null) {
            p.bufferLog(new PilotLogEntry(level, message, null, null, metadata, p.resolveLogAttributes()));
        }
    }

    /**
     * Send a log entry with category, thread, and metadata.
     */
    public static void log(@NonNull PilotLogLevel level, @NonNull String message,
                           @Nullable String category, @Nullable String thread,
                           @Nullable JSONObject metadata) {
        Pilot p = s_instance;
        if (p != null) {
            p.bufferLog(new PilotLogEntry(level, message, category, thread, metadata, p.resolveLogAttributes()));
        }
    }

    /**
     * Send a pre-built log entry.
     */
    public static void log(@NonNull PilotLogEntry entry) {
        Pilot p = s_instance;
        if (p != null) {
            p.bufferLog(entry);
        }
    }

    /**
     * Acknowledge an action from the dashboard.
     *
     * @param actionId   ID of the action to acknowledge
     * @param ackPayload Optional response payload (can be null)
     */
    public static void acknowledgeAction(@NonNull String actionId, @Nullable JSONObject ackPayload) {
        Pilot p = s_instance;
        if (p == null) {
            return;
        }
        String token = p.m_sessionToken.get();
        if (token == null) {
            return;
        }
        p.m_executor.execute(() -> {
            try {
                p.m_httpClient.acknowledgeAction(token, actionId, ackPayload);
            } catch (PilotException e) {
                PilotLog.e("Failed to acknowledge action", e);
            }
        });
    }

    /**
     * Shut down the SDK completely. Closes the session if active, stops all threads.
     * After shutdown, {@link #initialize} can be called again.
     */
    public static void shutdown() {
        Pilot p;
        synchronized (Pilot.class) {
            p = s_instance;
            s_instance = null;
        }
        if (p != null) {
            p.doShutdown();
        }
    }

    // ══════════════════════════════════════════════════════
    //  Internal
    // ══════════════════════════════════════════════════════

    private static Pilot requireInstance() {
        Pilot p = s_instance;
        if (p == null) {
            throw new IllegalStateException("Pilot.initialize() must be called first");
        }
        return p;
    }

    private void startConnection() {
        if (m_running.compareAndSet(false, true) == false) {
            PilotLog.w("Already connecting/connected");
            return;
        }

        m_executor = Executors.newScheduledThreadPool(3);
        m_executor.execute(this::connectAndWaitApproval);
    }

    private void stopConnection() {
        if (m_running.compareAndSet(true, false) == false) {
            return;
        }

        cancelScheduledTasks();

        String token = m_sessionToken.getAndSet(null);
        if (token != null && m_executor != null && !m_executor.isShutdown()) {
            m_executor.execute(() -> {
                try {
                    flushLogs(token);
                    m_httpClient.closeSession(token);
                } catch (PilotException ignored) {
                }
                setStatus(PilotSessionStatus.CLOSED);
                notifySessionClosed();
            });
        } else {
            setStatus(PilotSessionStatus.DISCONNECTED);
        }

        shutdownExecutor();
    }

    private void doShutdown() {
        PilotLog.i("Shutting down Pilot SDK");
        stopConnection();
        m_httpClient.shutdown();
        m_actionListeners.clear();
        m_sessionListeners.clear();
        m_logBuffer.clear();
    }

    private void connectAndWaitApproval() {
        int retryCount = 0;
        long retryDelayMs = 2000;
        final long maxRetryDelayMs = 30000;

        while (m_running.get()) {
            try {
                doConnectAndWaitApproval();
                return; // success
            } catch (PilotException e) {
                if (e.isUnauthorized()) {
                    PilotLog.e("Authentication failed", e);
                    setStatus(PilotSessionStatus.ERROR);
                    m_running.set(false);
                    notifyAuthFailed();
                    notifyError(e);
                    return;
                }

                retryCount++;
                PilotLog.w("Connection attempt %d failed: %s, retrying in %dms", retryCount, e.getMessage(), retryDelayMs);
                setStatus(PilotSessionStatus.CONNECTING);

                try {
                    Thread.sleep(retryDelayMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    m_running.set(false);
                    return;
                }

                retryDelayMs = Math.min(retryDelayMs * 2, maxRetryDelayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                m_running.set(false);
                return;
            }
        }
    }

    private void doConnectAndWaitApproval() throws PilotException, InterruptedException {
        setStatus(PilotSessionStatus.CONNECTING);
        notifyConnecting();

        String deviceId = m_config.deviceId;
        String deviceName = m_config.deviceName;

        if (deviceId == null || deviceId.isEmpty()) {
            deviceId = Build.BRAND + "-" + Build.MODEL;
        }
        if (deviceName == null || deviceName.isEmpty()) {
            deviceName = Build.MODEL + " (Android " + Build.VERSION.RELEASE + ")";
        }

        PilotConnectResponse resp = m_httpClient.connect(deviceId, deviceName, resolveAllSessionAttributes());
        m_requestId.set(resp.getRequestId());

        PilotLog.i("Connect request sent, request_id=%s, status=%s", resp.getRequestId(), resp.getStatus());

        if (resp.isApproved() && resp.getSessionToken() != null) {
            onApproved(resp.getSessionToken());
            return;
        }

        if (resp.isRejected()) {
            onRejected();
            return;
        }

        setStatus(PilotSessionStatus.WAITING_APPROVAL);
        notifyWaitingApproval(resp.getRequestId());

        // Poll loop
        while (m_running.get()) {
            Thread.sleep(m_config.pollIntervalMs);

            PilotConnectResponse pollResp = m_httpClient.pollStatus(resp.getRequestId());

            if (pollResp.isApproved() && pollResp.getSessionToken() != null) {
                onApproved(pollResp.getSessionToken());
                return;
            }

            if (pollResp.isRejected()) {
                onRejected();
                return;
            }
        }
    }

    private void onApproved(@NonNull String sessionToken) {
        m_sessionToken.set(sessionToken);
        setStatus(PilotSessionStatus.ACTIVE);

        PilotLog.i("Session approved and active");

        notifySessionStarted(sessionToken);

        // Send UI (tab-based) if any tabs exist
        if (m_ui.hasTabs()) {
            JSONObject snapshot = buildUISnapshotOnMainThread();
            if (snapshot != null) {
                try {
                    m_httpClient.submitPanel(sessionToken, snapshot);
                    m_ui.markSent();
                    PilotLog.d("Initial UI submitted (revision=%d)", m_ui.getRevision());
                } catch (PilotException e) {
                    PilotLog.e("Failed to submit initial UI", e);
                    notifyError(e);
                }
            }
        }

        // Start background tasks
        m_heartbeatFuture = m_executor.scheduleAtFixedRate(
                () -> doHeartbeat(sessionToken),
                m_config.heartbeatIntervalMs,
                m_config.heartbeatIntervalMs,
                TimeUnit.MILLISECONDS
        );

        m_actionPollFuture = m_executor.scheduleAtFixedRate(
                () -> doPollActions(sessionToken),
                0,
                m_config.actionPollIntervalMs,
                TimeUnit.MILLISECONDS
        );

        m_logFlushFuture = m_executor.scheduleAtFixedRate(
                () -> flushLogs(sessionToken),
                m_config.logFlushIntervalMs,
                m_config.logFlushIntervalMs,
                TimeUnit.MILLISECONDS
        );
    }

    private void onRejected() {
        PilotLog.w("Connection request rejected");
        setStatus(PilotSessionStatus.REJECTED);
        m_running.set(false);
        notifyRejected();
    }

    private void doHeartbeat(@NonNull String sessionToken) {
        if (!m_running.get()) return;

        Map<String, String> changedAttrs = resolveChangedSessionAttributes();

        try {
            m_httpClient.heartbeat(sessionToken, changedAttrs);
        } catch (PilotException e) {
            PilotLog.e("Heartbeat failed", e);
            if (e.isSessionGone()) {
                handleSessionGone();
            } else {
                notifyError(e);
            }
        }
    }

    private void doPollActions(@NonNull String sessionToken) {
        if (!m_running.get()) return;

        // Poll value providers + snapshot UI on Main Thread
        JSONObject uiSnapshot = snapshotUIOnMainThread();

        // Send UI if snapshot was produced (means there were unsent changes)
        if (uiSnapshot != null) {
            try {
                m_httpClient.submitPanel(sessionToken, uiSnapshot);
                m_ui.markSent();
                PilotLog.d("UI submitted (revision=%d)", m_ui.getRevision());
            } catch (PilotException e) {
                PilotLog.e("Failed to submit UI", e);
                notifyError(e);
            }
        }

        try {
            JSONObject json = m_httpClient.pollActions(sessionToken);
            JSONArray actionsArr = json.optJSONArray("actions");
            if (actionsArr != null && actionsArr.length() > 0) {
                for (int i = 0; i < actionsArr.length(); i++) {
                    JSONObject actionJson = actionsArr.optJSONObject(i);
                    if (actionJson != null) {
                        PilotAction action = PilotAction.fromJson(actionJson);
                        dispatchActionOnMainThread(action);
                    }
                }
            }
        } catch (PilotException e) {
            if (e.isSessionGone()) {
                handleSessionGone();
            } else {
                PilotLog.e("Action poll failed", e);
            }
        }
    }

    /**
     * Run pollValues + toJson on the Main Thread to avoid races with UI mutations.
     * Returns null if there are no unsent changes.
     */
    @Nullable
    private JSONObject snapshotUIOnMainThread() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            m_ui.pollValues();
            return m_ui.hasUnsent() ? m_ui.toJson() : null;
        }

        FutureTask<JSONObject> task = new FutureTask<>(() -> {
            m_ui.pollValues();
            return m_ui.hasUnsent() ? m_ui.toJson() : null;
        });
        m_mainHandler.post(task);

        try {
            return task.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } catch (ExecutionException e) {
            PilotLog.e("Failed to snapshot UI on main thread", e);
            return null;
        }
    }

    /**
     * Build a UI snapshot on the Main Thread unconditionally (for initial submit).
     */
    @Nullable
    private JSONObject buildUISnapshotOnMainThread() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            return m_ui.toJson();
        }

        FutureTask<JSONObject> task = new FutureTask<>(() -> m_ui.toJson());
        m_mainHandler.post(task);

        try {
            return task.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } catch (ExecutionException e) {
            PilotLog.e("Failed to build UI snapshot on main thread", e);
            return null;
        }
    }

    /**
     * Dispatch an action callback on the Main Thread so that any UI mutations
     * triggered by the callback stay on the same thread as other UI operations.
     */
    private void dispatchActionOnMainThread(@NonNull PilotAction action) {
        m_mainHandler.post(() -> {
            try {
                m_ui.dispatchAction(action);
            } catch (Exception ex) {
                PilotLog.e("Widget callback threw exception", ex);
            }

            for (PilotActionListener listener : m_actionListeners) {
                try {
                    listener.onPilotActionReceived(action);
                } catch (Exception ex) {
                    PilotLog.e("Action listener threw exception", ex);
                }
            }
        });
    }

    private void bufferLog(@NonNull PilotLogEntry entry) {
        synchronized (m_logBuffer) {
            if (m_logBuffer.size() >= m_config.logBufferSize) {
                m_logBuffer.remove(0);

                if (m_logOverflowWarned.compareAndSet(false, true)) {
                    PilotLog.w("Log buffer overflow (" + m_config.logBufferSize + "), dropping oldest entries");
                }
            }

            m_logBuffer.add(entry);
        }
    }

    private void flushLogs(@NonNull String sessionToken) {
        if (m_logBuffer.isEmpty()) return;

        List<PilotLogEntry> chunk;
        synchronized (m_logBuffer) {
            int count = Math.min(m_logBuffer.size(), m_config.logBatchSize);
            chunk = new ArrayList<>(m_logBuffer.subList(0, count));
            m_logBuffer.subList(0, count).clear();
        }

        try {
            m_httpClient.sendLogs(sessionToken, chunk);

            m_logOverflowWarned.set(false);
        } catch (PilotException e) {
            PilotLog.e("Failed to flush logs", e);

            if (e.isNetworkError() || e.getHttpCode() >= 500) {
                synchronized (m_logBuffer) {
                    m_logBuffer.addAll(0, chunk);

                    // Trim to max buffer size
                    while (m_logBuffer.size() > m_config.logBufferSize) {
                        m_logBuffer.remove(m_logBuffer.size() - 1);
                    }
                }
            }
        }
    }

    @Nullable
    private JSONObject resolveLogAttributes() {
        PilotLogAttributeBuilder builder = m_config.logAttributes;
        Map<String, String> staticAttrs = builder.getStaticAttributes();
        Map<String, PilotValueProvider> dynamicAttrs = builder.getDynamicAttributes();

        if (staticAttrs.isEmpty() && dynamicAttrs.isEmpty()) {
            return null;
        }

        JSONObject attributes = new JSONObject();
        try {
            for (Map.Entry<String, String> entry : staticAttrs.entrySet()) {
                attributes.put(entry.getKey(), entry.getValue());
            }
        } catch (Exception ignored) {
        }

        for (Map.Entry<String, PilotValueProvider> entry : dynamicAttrs.entrySet()) {
            try {
                Object value = entry.getValue().getValue();
                if (value != null) {
                    attributes.put(entry.getKey(), String.valueOf(value));
                }
            } catch (Exception ignored) {
            }
        }

        if (attributes.length() == 0) {
            return null;
        }

        return attributes;
    }

    private Map<String, String> resolveAllSessionAttributes() {
        PilotSessionAttributeBuilder builder = m_config.sessionAttributes;
        Map<String, String> merged = new ConcurrentHashMap<>(builder.getStaticAttributes());

        for (Map.Entry<String, PilotValueProvider> entry : builder.getDynamicAttributes().entrySet()) {
            try {
                Object value = entry.getValue().getValue();
                String str = String.valueOf(value);
                merged.put(entry.getKey(), str);
                m_sessionAttributeCache.put(entry.getKey(), str);
            } catch (Exception e) {
                PilotLog.e("Session attribute provider failed: " + entry.getKey(), e);
            }
        }

        return merged;
    }

    @Nullable
    private Map<String, String> resolveChangedSessionAttributes() {
        Map<String, PilotValueProvider> dynamicAttrs = m_config.sessionAttributes.getDynamicAttributes();
        if (dynamicAttrs.isEmpty()) return null;

        Map<String, String> changed = null;

        for (Map.Entry<String, PilotValueProvider> entry : dynamicAttrs.entrySet()) {
            try {
                Object value = entry.getValue().getValue();
                String str = String.valueOf(value);
                String cached = m_sessionAttributeCache.get(entry.getKey());

                if (!str.equals(cached)) {
                    m_sessionAttributeCache.put(entry.getKey(), str);
                    if (changed == null) {
                        changed = new ConcurrentHashMap<>();
                    }
                    changed.put(entry.getKey(), str);
                }
            } catch (Exception e) {
                PilotLog.e("Session attribute provider failed: " + entry.getKey(), e);
            }
        }

        return changed;
    }

    private void handleSessionGone() {
        PilotLog.w("Session is gone (410), stopping");
        m_running.set(false);
        m_sessionToken.set(null);
        cancelScheduledTasks();
        setStatus(PilotSessionStatus.CLOSED);
        notifySessionClosed();
    }

    private void setStatus(@NonNull PilotSessionStatus status) {
        m_status.set(status);
    }

    private void cancelScheduledTasks() {
        if (m_heartbeatFuture != null) {
            m_heartbeatFuture.cancel(false);
            m_heartbeatFuture = null;
        }
        if (m_actionPollFuture != null) {
            m_actionPollFuture.cancel(false);
            m_actionPollFuture = null;
        }
        if (m_logFlushFuture != null) {
            m_logFlushFuture.cancel(false);
            m_logFlushFuture = null;
        }
    }

    private void shutdownExecutor() {
        if (m_executor != null && !m_executor.isShutdown()) {
            m_executor.shutdown();
        }
    }

    // ── Listener notifications ──

    private void notifyConnecting() {
        for (PilotSessionListener l : m_sessionListeners) {
            l.onPilotSessionConnecting();
        }
    }

    private void notifyWaitingApproval(@NonNull String requestId) {
        for (PilotSessionListener l : m_sessionListeners) {
            l.onPilotSessionWaitingApproval(requestId);
        }
    }

    private void notifySessionStarted(@NonNull String sessionToken) {
        for (PilotSessionListener l : m_sessionListeners) {
            l.onPilotSessionStarted(sessionToken);
        }
    }

    private void notifySessionClosed() {
        for (PilotSessionListener l : m_sessionListeners) {
            l.onPilotSessionClosed();
        }
    }

    private void notifyRejected() {
        for (PilotSessionListener l : m_sessionListeners) {
            l.onPilotSessionRejected();
        }
    }

    private void notifyAuthFailed() {
        for (PilotSessionListener l : m_sessionListeners) {
            l.onPilotSessionAuthFailed();
        }
    }

    private void notifyError(@NonNull PilotException exception) {
        for (PilotSessionListener l : m_sessionListeners) {
            l.onPilotSessionError(exception);
        }
    }
}
