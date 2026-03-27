package org.pilot.sdk;

import androidx.annotation.NonNull;

/**
 * Describes a type of metric that can be recorded.
 * Use the built-in constants for standard system metrics,
 * or create custom types for application-specific metrics.
 *
 * <pre>{@code
 * // Built-in types:
 * PilotMetricType.FPS          // aggregation = GAUGE
 * PilotMetricType.NETWORK_RX   // aggregation = RATE
 *
 * // Custom types:
 * PilotMetricType DRAW_CALLS = PilotMetricType.create("draw_calls");
 * PilotMetricType ERRORS = PilotMetricType.create("errors", "", PilotMetricAggregation.COUNTER);
 * }</pre>
 */
public final class PilotMetricType {
    private final String m_key;
    private final String m_unit;
    private final PilotMetricAggregation m_aggregation;

    // ── Built-in metric types ──

    public static final PilotMetricType FPS = new PilotMetricType("fps", "", PilotMetricAggregation.GAUGE);
    public static final PilotMetricType FRAME_TIME = new PilotMetricType("frame_time", "ms", PilotMetricAggregation.GAUGE);
    public static final PilotMetricType MEMORY = new PilotMetricType("memory", "bytes", PilotMetricAggregation.GAUGE);
    public static final PilotMetricType VIDEO_MEMORY = new PilotMetricType("video_memory", "bytes", PilotMetricAggregation.GAUGE);
    public static final PilotMetricType CPU_USAGE = new PilotMetricType("cpu_usage", "%", PilotMetricAggregation.GAUGE);
    public static final PilotMetricType NETWORK_RX = new PilotMetricType("network_rx", "bytes/s", PilotMetricAggregation.RATE);
    public static final PilotMetricType NETWORK_TX = new PilotMetricType("network_tx", "bytes/s", PilotMetricAggregation.RATE);
    public static final PilotMetricType BATTERY_LEVEL = new PilotMetricType("battery_level", "%", PilotMetricAggregation.GAUGE);
    public static final PilotMetricType BATTERY_CHARGING = new PilotMetricType("battery_charging", "", PilotMetricAggregation.GAUGE);
    public static final PilotMetricType DRAW_CALLS = new PilotMetricType("draw_calls", "", PilotMetricAggregation.GAUGE);
    public static final PilotMetricType THREAD_COUNT = new PilotMetricType("thread_count", "", PilotMetricAggregation.GAUGE);

    private PilotMetricType(@NonNull String key, @NonNull String unit, @NonNull PilotMetricAggregation aggregation) {
        m_key = key;
        m_unit = unit;
        m_aggregation = aggregation;
    }

    /**
     * Create a custom metric type with GAUGE aggregation.
     *
     * @param key unique key, sent to the server as metric_type
     */
    @NonNull
    public static PilotMetricType create(@NonNull String key) {
        return new PilotMetricType(key, "", PilotMetricAggregation.GAUGE);
    }

    /**
     * Create a custom metric type with a unit hint and GAUGE aggregation.
     *
     * @param key  unique key, sent to the server as metric_type
     * @param unit display unit (e.g. "ms", "%", "bytes")
     */
    @NonNull
    public static PilotMetricType create(@NonNull String key, @NonNull String unit) {
        return new PilotMetricType(key, unit, PilotMetricAggregation.GAUGE);
    }

    /**
     * Create a custom metric type with full control.
     *
     * @param key         unique key, sent to the server as metric_type
     * @param unit        display unit (e.g. "ms", "%", "bytes")
     * @param aggregation how the server should aggregate values
     */
    @NonNull
    public static PilotMetricType create(@NonNull String key, @NonNull String unit,
                                         @NonNull PilotMetricAggregation aggregation) {
        return new PilotMetricType(key, unit, aggregation);
    }

    /**
     * The key string sent to the server as {@code metric_type}.
     */
    @NonNull
    public String getKey() {
        return m_key;
    }

    /**
     * Display unit hint (e.g. "ms", "%", "bytes"). May be empty.
     */
    @NonNull
    public String getUnit() {
        return m_unit;
    }

    /**
     * How the server should aggregate values when compressing time ranges.
     */
    @NonNull
    public PilotMetricAggregation getAggregation() {
        return m_aggregation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PilotMetricType)) return false;
        return m_key.equals(((PilotMetricType) o).m_key);
    }

    @Override
    public int hashCode() {
        return m_key.hashCode();
    }

    @NonNull
    @Override
    public String toString() {
        return m_key;
    }
}
