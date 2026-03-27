package org.pilot.sdk;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.TrafficStats;
import android.os.BatteryManager;
import android.os.Debug;
import android.os.Process;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

/**
 * Built-in metric collector that automatically gathers system metrics:
 * <ul>
 *   <li><b>fps</b> — frames per second (via Choreographer)</li>
 *   <li><b>frame_time</b> — last frame duration in ms</li>
 *   <li><b>memory</b> — total used memory in bytes (Java heap + native heap)</li>
 *   <li><b>thread_count</b> — number of active threads</li>
 *   <li><b>network_rx</b> — received bytes/sec for this UID</li>
 *   <li><b>network_tx</b> — transmitted bytes/sec for this UID</li>
 *   <li><b>battery_level</b> — battery percentage 0–100 (requires Context)</li>
 *   <li><b>battery_charging</b> — 1 if charging, 0 otherwise (requires Context)</li>
 * </ul>
 */
final class PilotDefaultMetricCollector implements PilotMetricCollector {
    private final PilotFpsTracker m_fpsTracker;
    @Nullable
    private final Context m_context;

    // Network rate tracking
    private long m_prevRxBytes = -1;
    private long m_prevTxBytes = -1;
    private long m_prevNetworkTimeMs = 0;

    PilotDefaultMetricCollector(@NonNull PilotFpsTracker fpsTracker, @Nullable Context context) {
        m_fpsTracker = fpsTracker;
        m_context = context != null ? context.getApplicationContext() : null;
    }

    @Override
    public void collect(@NonNull List<PilotMetricEntry> out) {
        long now = System.currentTimeMillis();

        // FPS & frame time
        double fps = m_fpsTracker.getFps();
        double frameTime = m_fpsTracker.getFrameTimeMs();
        if (fps > 0) {
            out.add(new PilotMetricEntry(PilotMetricType.FPS, fps));
            out.add(new PilotMetricEntry(PilotMetricType.FRAME_TIME, frameTime));
        }

        // Memory: Java heap + native heap (bytes)
        Runtime runtime = Runtime.getRuntime();
        long javaUsed = runtime.totalMemory() - runtime.freeMemory();
        long nativeUsed = Debug.getNativeHeapAllocatedSize();
        out.add(new PilotMetricEntry(PilotMetricType.MEMORY, javaUsed + nativeUsed));

        // Thread count
        out.add(new PilotMetricEntry(PilotMetricType.THREAD_COUNT, Thread.activeCount()));

        // Network RX/TX (bytes per second)
        collectNetwork(out, now);

        // Battery (requires Context)
        if (m_context != null) {
            collectBattery(out);
        }
    }

    private void collectNetwork(@NonNull List<PilotMetricEntry> out, long nowMs) {
        int uid = Process.myUid();
        long rxBytes = TrafficStats.getUidRxBytes(uid);
        long txBytes = TrafficStats.getUidTxBytes(uid);

        if (rxBytes == TrafficStats.UNSUPPORTED || txBytes == TrafficStats.UNSUPPORTED) {
            return;
        }

        if (m_prevRxBytes >= 0 && m_prevNetworkTimeMs > 0) {
            long dtMs = nowMs - m_prevNetworkTimeMs;
            if (dtMs > 0) {
                double rxRate = ((rxBytes - m_prevRxBytes) * 1000.0) / dtMs;
                double txRate = ((txBytes - m_prevTxBytes) * 1000.0) / dtMs;
                out.add(new PilotMetricEntry(PilotMetricType.NETWORK_RX, Math.max(0, rxRate)));
                out.add(new PilotMetricEntry(PilotMetricType.NETWORK_TX, Math.max(0, txRate)));
            }
        }

        m_prevRxBytes = rxBytes;
        m_prevTxBytes = txBytes;
        m_prevNetworkTimeMs = nowMs;
    }

    @SuppressWarnings("deprecation")
    private void collectBattery(@NonNull List<PilotMetricEntry> out) {
        try {
            IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryStatus = m_context.registerReceiver(null, filter);
            if (batteryStatus == null) return;

            int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            int plugged = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);

            if (level >= 0 && scale > 0) {
                double pct = (level * 100.0) / scale;
                out.add(new PilotMetricEntry(PilotMetricType.BATTERY_LEVEL, pct));
            }

            out.add(new PilotMetricEntry(PilotMetricType.BATTERY_CHARGING, plugged > 0 ? 1.0 : 0.0));
        } catch (Exception e) {
            PilotLog.d("Battery metric collection failed: %s", e.getMessage());
        }
    }
}
