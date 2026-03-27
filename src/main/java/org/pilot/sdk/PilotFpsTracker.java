package org.pilot.sdk;

import android.os.Handler;
import android.os.Looper;
import android.view.Choreographer;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Tracks FPS and frame time using Android's {@link Choreographer}.
 * Runs callbacks on the main thread; stores latest values atomically
 * for thread-safe reading from background threads.
 */
final class PilotFpsTracker implements Choreographer.FrameCallback {
    private final Handler m_mainHandler = new Handler(Looper.getMainLooper());
    private final AtomicBoolean m_running = new AtomicBoolean(false);

    private volatile double m_fps = 0.0;
    private volatile double m_frameTimeMs = 0.0;

    private long m_prevFrameNanos = 0;
    private int m_frameCount = 0;
    private long m_fpsWindowStartNanos = 0;

    // FPS is calculated over a rolling window
    private static final long FPS_WINDOW_NANOS = 500_000_000L; // 0.5 seconds

    void start() {
        if (m_running.compareAndSet(false, true)) {
            m_mainHandler.post(() -> {
                m_prevFrameNanos = 0;
                m_frameCount = 0;
                m_fpsWindowStartNanos = 0;
                Choreographer.getInstance().postFrameCallback(this);
            });
        }
    }

    void stop() {
        if (m_running.compareAndSet(true, false)) {
            m_mainHandler.post(() ->
                    Choreographer.getInstance().removeFrameCallback(this)
            );
        }
    }

    @Override
    public void doFrame(long frameTimeNanos) {
        if (!m_running.get()) return;

        if (m_prevFrameNanos > 0) {
            long deltaNanos = frameTimeNanos - m_prevFrameNanos;
            m_frameTimeMs = deltaNanos / 1_000_000.0;
        }

        m_prevFrameNanos = frameTimeNanos;
        m_frameCount++;

        if (m_fpsWindowStartNanos == 0) {
            m_fpsWindowStartNanos = frameTimeNanos;
        } else {
            long elapsed = frameTimeNanos - m_fpsWindowStartNanos;
            if (elapsed >= FPS_WINDOW_NANOS) {
                m_fps = (m_frameCount * 1_000_000_000.0) / elapsed;
                m_frameCount = 0;
                m_fpsWindowStartNanos = frameTimeNanos;
            }
        }

        Choreographer.getInstance().postFrameCallback(this);
    }

    double getFps() {
        return m_fps;
    }

    double getFrameTimeMs() {
        return m_frameTimeMs;
    }
}
