package org.pilot.sdk;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
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
 * // 3. Build and submit panel
 * PilotPanel panel = new PilotPanel();
 * panel.addSection("controls", "Controls")
 *     .addButton("btn-restart", "Restart", "contained", "error");
 * Pilot.submitPanel(panel);
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
    private static volatile Pilot s_instance;

    private final PilotConfig m_config;
    private final PilotHttpClient m_httpClient;

    private final AtomicReference<String> m_sessionToken = new AtomicReference<>(null);
    private final AtomicReference<String> m_requestId = new AtomicReference<>(null);
    private final AtomicReference<PilotSessionStatus> m_status = new AtomicReference<>(PilotSessionStatus.DISCONNECTED);
    private final AtomicBoolean m_running = new AtomicBoolean(false);

    private final List<PilotLogEntry> m_logBuffer = Collections.synchronizedList(new ArrayList<>());

    private final CopyOnWriteArrayList<PilotActionListener> m_actionListeners = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<PilotSessionListener> m_sessionListeners = new CopyOnWriteArrayList<>();

    private ScheduledExecutorService m_executor;
    private ScheduledFuture<?> m_heartbeatFuture;
    private ScheduledFuture<?> m_actionPollFuture;
    private ScheduledFuture<?> m_logFlushFuture;

    private PilotPanel m_currentPanel;

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
                s_instance = new Pilot(config);
                PilotLog.i("Pilot SDK initialized (server: %s)", config.baseUrl);
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
     * Submit a debug panel layout. If a session is active, sends immediately.
     * If not yet connected, the panel will be sent once the session starts.
     */
    public static void submitPanel(@NonNull PilotPanel panel) {
        Pilot p = requireInstance();
        p.m_currentPanel = panel;

        String token = p.m_sessionToken.get();
        if (token != null) {
            p.doSubmitPanel(token, panel);
        }
    }

    /**
     * Increment panel revision and re-submit. Useful after updating widget values.
     */
    public static void updatePanel() {
        Pilot p = requireInstance();
        PilotPanel panel = p.m_currentPanel;
        if (panel != null) {
            panel.incrementRevision();
            String token = p.m_sessionToken.get();
            if (token != null) {
                p.doSubmitPanel(token, panel);
            }
        }
    }

    /**
     * Start connecting to the Pilot server. Runs asynchronously.
     * This will:
     * 1. POST /api/client/connect
     * 2. Poll for approval
     * 3. Once approved — start heartbeat, action polling, and log flushing
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
     */
    public static void log(@NonNull PilotLogLevel level, @NonNull String message) {
        Pilot p = s_instance;
        if (p != null) {
            p.m_logBuffer.add(new PilotLogEntry(level, message, null));
        }
    }

    /**
     * Send a log entry with metadata.
     */
    public static void log(@NonNull PilotLogLevel level, @NonNull String message, @Nullable JSONObject metadata) {
        Pilot p = s_instance;
        if (p != null) {
            p.m_logBuffer.add(new PilotLogEntry(level, message, metadata));
        }
    }

    /**
     * Send a pre-built log entry.
     */
    public static void log(@NonNull PilotLogEntry entry) {
        Pilot p = s_instance;
        if (p != null) {
            p.m_logBuffer.add(entry);
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
        m_currentPanel = null;
    }

    private void connectAndWaitApproval() {
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

        try {
            PilotConnectResponse resp = m_httpClient.connect(deviceId, deviceName);
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
        } catch (PilotException e) {
            PilotLog.e("Connection failed", e);
            setStatus(PilotSessionStatus.ERROR);
            m_running.set(false);
            notifyError(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            m_running.set(false);
        }
    }

    private void onApproved(@NonNull String sessionToken) {
        m_sessionToken.set(sessionToken);
        setStatus(PilotSessionStatus.ACTIVE);

        PilotLog.i("Session approved and active");

        notifySessionStarted(sessionToken);

        // Send pending panel
        PilotPanel panel = m_currentPanel;
        if (panel != null) {
            doSubmitPanel(sessionToken, panel);
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
        try {
            m_httpClient.heartbeat(sessionToken);
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
        try {
            JSONObject json = m_httpClient.pollActions(sessionToken);
            JSONArray actionsArr = json.optJSONArray("actions");
            if (actionsArr != null && actionsArr.length() > 0) {
                for (int i = 0; i < actionsArr.length(); i++) {
                    JSONObject actionJson = actionsArr.optJSONObject(i);
                    if (actionJson != null) {
                        PilotAction action = PilotAction.fromJson(actionJson);
                        for (PilotActionListener listener : m_actionListeners) {
                            try {
                                listener.onAction(action);
                            } catch (Exception ex) {
                                PilotLog.e("Action listener threw exception", ex);
                            }
                        }
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

    private void doSubmitPanel(@NonNull String sessionToken, @NonNull PilotPanel panel) {
        if (m_executor == null || m_executor.isShutdown()) return;
        m_executor.execute(() -> {
            try {
                m_httpClient.submitPanel(sessionToken, panel.toJson());
                PilotLog.d("Panel submitted (revision=%d)", panel.getRevision());
            } catch (PilotException e) {
                PilotLog.e("Failed to submit panel", e);
                notifyError(e);
            }
        });
    }

    private void flushLogs(@NonNull String sessionToken) {
        if (m_logBuffer.isEmpty()) return;

        List<PilotLogEntry> batch;
        synchronized (m_logBuffer) {
            batch = new ArrayList<>(m_logBuffer);
            m_logBuffer.clear();
        }

        try {
            m_httpClient.sendLogs(sessionToken, batch);
        } catch (PilotException e) {
            PilotLog.e("Failed to flush logs", e);
            // Re-add failed logs to buffer
            synchronized (m_logBuffer) {
                m_logBuffer.addAll(0, batch);
            }
        }
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
            l.onConnecting();
        }
    }

    private void notifyWaitingApproval(@NonNull String requestId) {
        for (PilotSessionListener l : m_sessionListeners) {
            l.onWaitingApproval(requestId);
        }
    }

    private void notifySessionStarted(@NonNull String sessionToken) {
        for (PilotSessionListener l : m_sessionListeners) {
            l.onSessionStarted(sessionToken);
        }
    }

    private void notifySessionClosed() {
        for (PilotSessionListener l : m_sessionListeners) {
            l.onSessionClosed();
        }
    }

    private void notifyRejected() {
        for (PilotSessionListener l : m_sessionListeners) {
            l.onRejected();
        }
    }

    private void notifyError(@NonNull PilotException exception) {
        for (PilotSessionListener l : m_sessionListeners) {
            l.onError(exception);
        }
    }
}
