package org.pilot.sdk;

import androidx.annotation.NonNull;

import java.util.List;

/**
 * Interface for custom metric collectors.
 * Implement this to provide application-specific metrics (FPS, memory, etc.).
 *
 * <p>Collectors are called on a background thread at the configured sample interval.</p>
 *
 * <pre>{@code
 * Pilot.getMetrics().addCollector(new PilotMetricCollector() {
 *     @Override
 *     public void collect(List<PilotMetricEntry> out) {
 *         out.add(new PilotMetricEntry(PilotMetricType.FPS, getCurrentFps()));
 *         out.add(new PilotMetricEntry(PilotMetricType.MEMORY, Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()));
 *     }
 * });
 * }</pre>
 */
public interface PilotMetricCollector {
    /**
     * Called periodically to collect metric samples.
     * Add entries to the output list.
     *
     * @param out list to add metric entries to
     */
    void collect(@NonNull List<PilotMetricEntry> out);
}
