package org.pilot.sdk;

/**
 * Defines how the server should aggregate metric values when zooming out
 * or compressing time ranges.
 *
 * <ul>
 *   <li>{@link #GAUGE} — last value wins (FPS, memory, battery level)</li>
 *   <li>{@link #COUNTER} — values are summed (request count, errors)</li>
 *   <li>{@link #RATE} — values represent a per-second rate (network bytes/s)</li>
 * </ul>
 */
public enum PilotMetricAggregation {
    /** Snapshot of a current value. When aggregating, use the last (or average) value. */
    GAUGE("gauge"),

    /** Cumulative counter. When aggregating, values are summed. */
    COUNTER("counter"),

    /** Per-second rate. When aggregating, values are averaged. */
    RATE("rate");

    private final String m_key;

    PilotMetricAggregation(String key) {
        m_key = key;
    }

    /**
     * The wire key sent to the server.
     */
    public String getKey() {
        return m_key;
    }
}
