package org.pilot.sdk;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

final class PilotLiveManager {
    interface Callback {
        void onLiveModeChanged(boolean enabled, long actionPollIntervalMs);
    }

    private final Handler m_mainHandler = new Handler(Looper.getMainLooper());
    private final ScheduledExecutorService m_executor = Executors.newSingleThreadScheduledExecutor();
    private final PilotHttpClient m_httpClient;
    private final Callback m_callback;
    private final AtomicBoolean m_isLive = new AtomicBoolean(false);

    @Nullable
    private final Application m_application;
    @Nullable
    private final Application.ActivityLifecycleCallbacks m_lifecycleCallbacks;
    @Nullable
    private final PilotLiveKitPublisher m_liveKitPublisher;

    private volatile WeakReference<Activity> m_currentActivity = new WeakReference<>(null);
    private volatile WeakReference<PilotLiveOverlayView> m_overlayView = new WeakReference<>(null);
    private volatile LiveSettings m_settings = LiveSettings.low();

    PilotLiveManager(@Nullable Context context,
                       @NonNull PilotHttpClient httpClient,
                       @NonNull Callback callback) {
        m_httpClient = httpClient;
        m_callback = callback;

        Application application = resolveApplication(context);
        m_application = application;
        m_liveKitPublisher = application != null ? new PilotLiveKitPublisher(application) : null;

        if (application != null) {
            Application.ActivityLifecycleCallbacks lifecycleCallbacks = new Application.ActivityLifecycleCallbacks() {
                @Override
                public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
                }

                @Override
                public void onActivityStarted(@NonNull Activity activity) {
                }

                @Override
                public void onActivityResumed(@NonNull Activity activity) {
                    m_currentActivity = new WeakReference<>(activity);
                    attachOverlay(activity);
                }

                @Override
                public void onActivityPaused(@NonNull Activity activity) {
                    Activity current = m_currentActivity.get();
                    if (current == activity) {
                        m_currentActivity = new WeakReference<>(null);
                    }
                }

                @Override
                public void onActivityStopped(@NonNull Activity activity) {
                }

                @Override
                public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
                }

                @Override
                public void onActivityDestroyed(@NonNull Activity activity) {
                    PilotLiveOverlayView overlayView = m_overlayView.get();
                    if (overlayView != null && overlayView.getContext() == activity) {
                        removeOverlay(overlayView);
                        m_overlayView = new WeakReference<>(null);
                    }

                    Activity current = m_currentActivity.get();
                    if (current == activity) {
                        m_currentActivity = new WeakReference<>(null);
                    }
                }
            };

            application.registerActivityLifecycleCallbacks(lifecycleCallbacks);
            m_lifecycleCallbacks = lifecycleCallbacks;
        } else {
            m_lifecycleCallbacks = null;
        }
    }

    @NonNull
    JSONObject start(@NonNull String sessionToken, @Nullable JSONObject payload) {
        if (m_application == null || m_liveKitPublisher == null) {
            return buildAck(false, "Live requires Pilot.initialize(config, applicationContext)");
        }

        boolean wasLive = m_isLive.get();
        stopLiveRuntime();
        if (wasLive) {
            m_callback.onLiveModeChanged(false, 0L);
        }

        try {
            PublisherSession publisherSession = fetchPublisherSession(sessionToken, LiveSettings.fromPayload(payload));
            LiveSettings settings = publisherSession.m_settings;

            m_liveKitPublisher.start(
                    publisherSession.m_serverUrl,
                    publisherSession.m_participantToken
            );

            m_settings = settings;
            m_isLive.set(true);
            requestScreenCapturePermission();
            m_callback.onLiveModeChanged(true, settings.m_actionPollIntervalMs);

            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("preset", settings.m_presetName);
            metadata.put("max_dimension", settings.m_maxDimension);
            metadata.put("fps", settings.m_framesPerSecond);
            if (publisherSession.m_roomName != null) {
                metadata.put("room_name", publisherSession.m_roomName);
            }
            if (publisherSession.m_participantIdentity != null) {
                metadata.put("participant_identity", publisherSession.m_participantIdentity);
            }
            metadata.put("video_track_name", publisherSession.m_videoTrackName);
            Pilot.event("live_started", "live", metadata);

            JSONObject ack = buildAck(true, "live_started");
            try {
                ack.put("preset", settings.m_presetName);
                ack.put("max_dimension", settings.m_maxDimension);
                ack.put("fps", settings.m_framesPerSecond);
                ack.put("room_name", publisherSession.m_roomName);
                ack.put("video_track_name", publisherSession.m_videoTrackName);
                ack.put("waiting_for_activity", getCurrentActivity() == null);
            } catch (Exception ignored) {
            }
            return ack;
        } catch (PilotException e) {
            stopLiveRuntime();
            PilotLog.e("Failed to start LiveKit live", e);

            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("message", e.getMessage());
            metadata.put("http_code", e.getHttpCode());
            Pilot.event("live_start_failed", "live", metadata);
            return buildAck(false, e.getMessage() != null ? e.getMessage() : "Failed to start live");
        } catch (Exception e) {
            stopLiveRuntime();
            PilotLog.e("Failed to start LiveKit live", e);

            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("message", e.getMessage());
            Pilot.event("live_start_failed", "live", metadata);
            return buildAck(false, e.getMessage() != null ? e.getMessage() : "Failed to start live");
        }
    }

    @NonNull
    JSONObject stop() {
        boolean wasLive = m_isLive.get();
        stopLiveRuntime();
        m_callback.onLiveModeChanged(false, 0L);

        if (wasLive) {
            Pilot.event("live_stopped", "live", null);
        }

        return buildAck(true, wasLive ? "live_stopped" : "live_already_stopped");
    }

    void onSessionClosed() {
        stopLiveRuntime();
    }

    void shutdown() {
        onSessionClosed();

        if (m_application != null && m_lifecycleCallbacks != null) {
            m_application.unregisterActivityLifecycleCallbacks(m_lifecycleCallbacks);
        }

        m_executor.shutdownNow();
    }

    @NonNull
    JSONObject tap(@Nullable JSONObject payload) {
        if (!m_isLive.get()) {
            return buildAck(false, "Live is not active");
        }

        Activity activity = getCurrentActivity();
        if (activity == null) {
            return buildAck(false, "No resumed activity available for tap");
        }

        TouchPoint point = resolveTouchPoint(activity, payload);
        if (point == null) {
            return buildAck(false, "Unable to resolve tap coordinates");
        }

        long downTime = SystemClock.uptimeMillis();
        dispatchTouchEvent(activity, MotionEvent.ACTION_DOWN, downTime, downTime, point);
        dispatchTouchEvent(activity, MotionEvent.ACTION_UP, downTime, downTime + 1L, point);
        showTapOverlay(activity, point);

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("normalized_x", point.m_normalizedX);
        metadata.put("normalized_y", point.m_normalizedY);
        metadata.put("x", point.m_x);
        metadata.put("y", point.m_y);
        Pilot.event("live_tap", "live_input", metadata);

        return buildAck(true, "tap_sent");
    }

    @NonNull
    JSONObject longPress(@Nullable JSONObject payload) {
        if (!m_isLive.get()) {
            return buildAck(false, "Live is not active");
        }

        Activity activity = getCurrentActivity();
        if (activity == null) {
            return buildAck(false, "No resumed activity available for long press");
        }

        TouchPoint point = resolveTouchPoint(activity, payload);
        if (point == null) {
            return buildAck(false, "Unable to resolve long press coordinates");
        }

        long durationMs = clampLong(payload != null ? payload.optLong("duration_ms", 800L) : 800L, 250L, 2000L);
        long downTime = SystemClock.uptimeMillis();
        dispatchTouchEvent(activity, MotionEvent.ACTION_DOWN, downTime, downTime, point);
        showPressOverlay(activity, point);

        m_mainHandler.postDelayed(() -> {
            dispatchTouchEvent(activity, MotionEvent.ACTION_UP, downTime, SystemClock.uptimeMillis(), point);
            showReleaseOverlay(activity, point);
        }, durationMs);

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("normalized_x", point.m_normalizedX);
        metadata.put("normalized_y", point.m_normalizedY);
        metadata.put("x", point.m_x);
        metadata.put("y", point.m_y);
        metadata.put("duration_ms", durationMs);
        Pilot.event("live_long_press", "live_input", metadata);

        JSONObject ack = buildAck(true, "long_press_sent");
        try {
            ack.put("duration_ms", durationMs);
        } catch (Exception ignored) {
        }
        return ack;
    }

    private void requestScreenCapturePermission() {
        PilotScreenCaptureActivity.s_callback = (resultCode, data) -> {
            if (resultCode == Activity.RESULT_OK && data != null) {
                m_executor.execute(() -> {
                    if (!m_isLive.get() || m_liveKitPublisher == null) {
                        return;
                    }
                    try {
                        m_liveKitPublisher.enableScreenShare(data);
                        Pilot.event("screen_share_enabled", "live", null);
                    } catch (Exception e) {
                        PilotLog.e("Failed to enable screen share", e);
                        stop();
                    }
                });
            } else {
                PilotLog.w("Screen capture permission denied");
                m_executor.execute(this::stop);
            }
        };

        Activity activity = getCurrentActivity();
        if (activity != null) {
            activity.startActivity(new Intent(activity, PilotScreenCaptureActivity.class));
        } else if (m_application != null) {
            Intent intent = new Intent(m_application, PilotScreenCaptureActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            m_application.startActivity(intent);
        } else {
            PilotLog.e("No context available to request screen capture permission");
            m_executor.execute(this::stop);
        }
    }

    private void stopLiveRuntime() {
        m_isLive.set(false);
        PilotScreenCaptureActivity.s_callback = null;
        clearOverlay();
        if (m_liveKitPublisher != null) {
            m_liveKitPublisher.stop();
        }
    }

    @NonNull
    private PublisherSession fetchPublisherSession(@NonNull String sessionToken,
                                                   @NonNull LiveSettings requestedSettings) throws PilotException {
        JSONObject response = m_httpClient.getLivePublisherState(sessionToken);
        String statusMessage = trimToNull(response.optString("status_message", null));

        if (!response.optBoolean("configured", false)) {
            throw new PilotException(statusMessage != null ? statusMessage : "LiveKit is not configured on the server");
        }

        if (!response.optBoolean("requested", false)) {
            throw new PilotException(statusMessage != null ? statusMessage : "Live request is no longer active");
        }

        String serverUrl = trimToNull(response.optString("server_url", null));
        String participantToken = trimToNull(response.optString("participant_token", null));
        String videoTrackName = trimToNull(response.optString("video_track_name", null));
        if (serverUrl == null || participantToken == null || videoTrackName == null) {
            throw new PilotException("Server returned incomplete live credentials");
        }

        String presetName = trimToNull(response.optString("preset", requestedSettings.m_presetName));
        if (presetName == null) {
            presetName = requestedSettings.m_presetName;
        }

        int maxDimension = clampInt(response.optInt("max_dimension", requestedSettings.m_maxDimension), 240, 1440);
        int fps = clampInt(response.optInt("fps", requestedSettings.m_framesPerSecond), 1, 6);
        long actionPollIntervalMs = clampLong(
                response.optLong("action_poll_interval_ms", requestedSettings.m_actionPollIntervalMs),
                200L,
                2000L
        );

        return new PublisherSession(
                serverUrl,
                participantToken,
                trimToNull(response.optString("room_name", null)),
                trimToNull(response.optString("participant_identity", null)),
                videoTrackName,
                new LiveSettings(presetName, maxDimension, fps, actionPollIntervalMs)
        );
    }

    @Nullable
    private Activity getCurrentActivity() {
        return m_currentActivity.get();
    }

    private void dispatchTouchEvent(@NonNull Activity activity,
                                    int action,
                                    long downTime,
                                    long eventTime,
                                    @NonNull TouchPoint point) {
        MotionEvent event = MotionEvent.obtain(downTime, eventTime, action, point.m_x, point.m_y, 0);
        event.setSource(InputDevice.SOURCE_TOUCHSCREEN);
        activity.dispatchTouchEvent(event);
        event.recycle();
    }

    @Nullable
    private TouchPoint resolveTouchPoint(@NonNull Activity activity, @Nullable JSONObject payload) {
        View rootView = activity.getWindow().getDecorView();
        int width = rootView.getWidth();
        int height = rootView.getHeight();
        if (width <= 0 || height <= 0) {
            return null;
        }

        double normalizedX = payload != null ? payload.optDouble("normalized_x", 0.5d) : 0.5d;
        double normalizedY = payload != null ? payload.optDouble("normalized_y", 0.5d) : 0.5d;
        float clampedX = (float) clampDouble(normalizedX, 0d, 1d);
        float clampedY = (float) clampDouble(normalizedY, 0d, 1d);
        float x = clampedX * width;
        float y = clampedY * height;

        return new TouchPoint(x, y, clampedX, clampedY);
    }

    private void showTapOverlay(@NonNull Activity activity, @NonNull TouchPoint point) {
        PilotLiveOverlayView overlayView = attachOverlay(activity);
        overlayView.showTap(point.m_x, point.m_y);
    }

    private void showPressOverlay(@NonNull Activity activity, @NonNull TouchPoint point) {
        PilotLiveOverlayView overlayView = attachOverlay(activity);
        overlayView.showPress(point.m_x, point.m_y);
    }

    private void showReleaseOverlay(@NonNull Activity activity, @NonNull TouchPoint point) {
        PilotLiveOverlayView overlayView = attachOverlay(activity);
        overlayView.showRelease(point.m_x, point.m_y);
    }

    private void clearOverlay() {
        m_mainHandler.post(() -> {
            PilotLiveOverlayView overlayView = m_overlayView.get();
            if (overlayView != null) {
                overlayView.clearIndicator();
            }
        });
    }

    @NonNull
    private PilotLiveOverlayView attachOverlay(@NonNull Activity activity) {
        PilotLiveOverlayView overlayView = m_overlayView.get();
        ViewGroup decorView = (ViewGroup) activity.getWindow().getDecorView();
        if (overlayView != null && overlayView.getParent() == decorView) {
            return overlayView;
        }

        if (overlayView != null) {
            removeOverlay(overlayView);
        }

        overlayView = new PilotLiveOverlayView(activity);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        decorView.addView(overlayView, params);
        m_overlayView = new WeakReference<>(overlayView);
        return overlayView;
    }

    private void removeOverlay(@NonNull PilotLiveOverlayView overlayView) {
        View parent = (View) overlayView.getParent();
        if (parent instanceof ViewGroup) {
            ((ViewGroup) parent).removeView(overlayView);
        }
    }

    @NonNull
    private static JSONObject buildAck(boolean ok, @NonNull String status) {
        JSONObject ack = new JSONObject();
        try {
            ack.put("ok", ok);
            ack.put("status", status);
        } catch (Exception ignored) {
        }
        return ack;
    }

    @Nullable
    private static Application resolveApplication(@Nullable Context context) {
        if (context == null) {
            return null;
        }

        if (context instanceof Application) {
            return (Application) context;
        }

        Context appContext = context.getApplicationContext();
        if (appContext instanceof Application) {
            return (Application) appContext;
        }

        return null;
    }

    private static int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static long clampLong(long value, long min, long max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double clampDouble(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    @Nullable
    private static String trimToNull(@Nullable String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static final class LiveSettings {
        private final String m_presetName;
        private final int m_maxDimension;
        private final int m_framesPerSecond;
        private final long m_actionPollIntervalMs;

        private LiveSettings(@NonNull String presetName,
                               int maxDimension,
                               int framesPerSecond,
                               long actionPollIntervalMs) {
            m_presetName = presetName;
            m_maxDimension = maxDimension;
            m_framesPerSecond = framesPerSecond;
            m_actionPollIntervalMs = actionPollIntervalMs;
        }

        @NonNull
        static LiveSettings low() {
            return new LiveSettings("low", 540, 2, 500L);
        }

        @NonNull
        static LiveSettings balanced() {
            return new LiveSettings("balanced", 720, 3, 400L);
        }

        @NonNull
        static LiveSettings high() {
            return new LiveSettings("high", 1080, 4, 300L);
        }

        @NonNull
        static LiveSettings fromPayload(@Nullable JSONObject payload) {
            LiveSettings base = low();
            if (payload == null) {
                return base;
            }

            String preset = payload.optString("preset", "low");
            if ("balanced".equals(preset)) {
                base = balanced();
            } else if ("high".equals(preset)) {
                base = high();
            } else {
                preset = base.m_presetName;
            }

            int maxDimension = clampInt(payload.optInt("max_dimension", base.m_maxDimension), 240, 1440);
            int fps = clampInt(payload.optInt("fps", base.m_framesPerSecond), 1, 6);
            long actionPollIntervalMs = clampLong(
                    payload.optLong("action_poll_interval_ms", base.m_actionPollIntervalMs),
                    200L,
                    2000L
            );

            return new LiveSettings(preset, maxDimension, fps, actionPollIntervalMs);
        }
    }

    private static final class PublisherSession {
        private final String m_serverUrl;
        private final String m_participantToken;
        @Nullable
        private final String m_roomName;
        @Nullable
        private final String m_participantIdentity;
        private final String m_videoTrackName;
        private final LiveSettings m_settings;

        private PublisherSession(@NonNull String serverUrl,
                                 @NonNull String participantToken,
                                 @Nullable String roomName,
                                 @Nullable String participantIdentity,
                                 @NonNull String videoTrackName,
                                 @NonNull LiveSettings settings) {
            m_serverUrl = serverUrl;
            m_participantToken = participantToken;
            m_roomName = roomName;
            m_participantIdentity = participantIdentity;
            m_videoTrackName = videoTrackName;
            m_settings = settings;
        }
    }

    private static final class TouchPoint {
        private final float m_x;
        private final float m_y;
        private final float m_normalizedX;
        private final float m_normalizedY;

        private TouchPoint(float x, float y, float normalizedX, float normalizedY) {
            m_x = x;
            m_y = y;
            m_normalizedX = normalizedX;
            m_normalizedY = normalizedY;
        }
    }
}