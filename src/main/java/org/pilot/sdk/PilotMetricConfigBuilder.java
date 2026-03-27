package org.pilot.sdk;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Builder for metric subsystem configuration.
 * Controls whether metrics are enabled, sampling rate, buffer sizes,
 * and allows registering custom collectors.
 *
 * <pre>{@code
 * PilotMetricConfigBuilder metricConfig = new PilotMetricConfigBuilder()
 *     .setEnabled(true)
 *     .setSampleIntervalMs(200)
 *     .setFlushIntervalMs(5000)
 *     .addCollector(out -> {
 *         out.add(new PilotMetricEntry(PilotMetricType.DRAW_CALLS, renderer.getDrawCalls()));
 *         out.add(new PilotMetricEntry(PilotMetricType.VIDEO_MEMORY, renderer.getVramUsage()));
 *     });
 *
 * PilotConfig config = new PilotConfig.Builder(url, token)
 *     .setMetricConfig(metricConfig)
 *     .build();
 * }</pre>
 */
public final class PilotMetricConfigBuilder {
    private boolean m_enabled = true;
    private long m_sampleIntervalMs = 200;
    private long m_flushIntervalMs = 5000;
    private int m_bufferSize = 2000;
    private int m_batchSize = 200;
    private final List<PilotMetricCollector> m_collectors = new ArrayList<>();

    /**
     * Enable or disable the metrics subsystem.
     * When enabled, built-in collectors automatically gather FPS, memory,
     * network, battery, and other system metrics.
     * Default: true.
     */
    @NonNull
    public PilotMetricConfigBuilder setEnabled(boolean enabled) {
        m_enabled = enabled;
        return this;
    }

    /**
     * Set the metric sample interval in milliseconds.
     * Controls how often metric collectors are polled.
     * Range: 100..1000 ms. Default: 200 ms.
     */
    @NonNull
    public PilotMetricConfigBuilder setSampleIntervalMs(long ms) {
        m_sampleIntervalMs = Math.max(100, Math.min(1000, ms));
        return this;
    }

    /**
     * Set the interval for flushing buffered metrics to the server.
     * Default: 5000 ms.
     */
    @NonNull
    public PilotMetricConfigBuilder setFlushIntervalMs(long ms) {
        m_flushIntervalMs = ms;
        return this;
    }

    /**
     * Set the maximum number of metric entries to buffer before dropping oldest.
     * Default: 2000.
     */
    @NonNull
    public PilotMetricConfigBuilder setBufferSize(int size) {
        m_bufferSize = size;
        return this;
    }

    /**
     * Set the max batch size for sending to the server in one request.
     * Default: 200.
     */
    @NonNull
    public PilotMetricConfigBuilder setBatchSize(int size) {
        m_batchSize = size;
        return this;
    }

    /**
     * Add a custom metric collector.
     * Collectors are called on a background thread at the configured sample interval.
     *
     * <pre>{@code
     * metricConfig.addCollector(out -> {
     *     out.add(new PilotMetricEntry(PilotMetricType.DRAW_CALLS, renderer.getDrawCalls()));
     *     out.add(new PilotMetricEntry(PilotMetricType.VIDEO_MEMORY, renderer.getVramUsage()));
     * });
     * }</pre>
     */
    @NonNull
    public PilotMetricConfigBuilder addCollector(@NonNull PilotMetricCollector collector) {
        m_collectors.add(collector);
        return this;
    }

    // ── Package-private getters for SDK internals ──

    boolean isEnabled() {
        return m_enabled;
    }

    long getSampleIntervalMs() {
        return m_sampleIntervalMs;
    }

    long getFlushIntervalMs() {
        return m_flushIntervalMs;
    }

    int getBufferSize() {
        return m_bufferSize;
    }

    int getBatchSize() {
        return m_batchSize;
    }

    @NonNull
    List<PilotMetricCollector> getCollectors() {
        return m_collectors;
    }
}
