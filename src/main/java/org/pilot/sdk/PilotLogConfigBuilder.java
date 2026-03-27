package org.pilot.sdk;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Builder for log subsystem configuration.
 * Controls whether log sending is enabled, log level, buffer/batch sizes, and log attributes.
 *
 * <pre>{@code
 * PilotLogConfigBuilder logConfig = new PilotLogConfigBuilder()
 *     .setEnabled(true)
 *     .setLogLevel(PilotLogLevel.INFO)
 *     .setAttributes(new PilotLogAttributeBuilder()
 *         .putProvider("screen_name", () -> currentScreen));
 *
 * PilotConfig config = new PilotConfig.Builder(url, token)
 *     .setLogConfig(logConfig)
 *     .build();
 * }</pre>
 */
public final class PilotLogConfigBuilder {
    private boolean m_enabled = true;
    private PilotLogLevel m_logLevel = PilotLogLevel.INFO;
    private int m_batchSize = 100;
    private int m_bufferSize = 1000;
    private PilotLogAttributeBuilder m_attributes = new PilotLogAttributeBuilder();

    /**
     * Enable or disable sending application logs to the Pilot server.
     * Default: true.
     */
    @NonNull
    public PilotLogConfigBuilder setEnabled(boolean enabled) {
        m_enabled = enabled;
        return this;
    }

    /**
     * Set the minimum log level. Messages below this level are dropped.
     * Default: {@link PilotLogLevel#INFO}.
     */
    @NonNull
    public PilotLogConfigBuilder setLogLevel(@NonNull PilotLogLevel level) {
        m_logLevel = level;
        return this;
    }

    /**
     * Set the max batch size for sending logs to the server in one request.
     * Default: 100.
     */
    @NonNull
    public PilotLogConfigBuilder setBatchSize(int size) {
        m_batchSize = size;
        return this;
    }

    /**
     * Set the maximum number of log entries to buffer before dropping oldest.
     * Default: 1000.
     */
    @NonNull
    public PilotLogConfigBuilder setBufferSize(int size) {
        m_bufferSize = size;
        return this;
    }

    /**
     * Set log attributes (static and dynamic) via a builder.
     * Static values are fixed; dynamic providers are resolved at each log() call.
     */
    @NonNull
    public PilotLogConfigBuilder setAttributes(@NonNull PilotLogAttributeBuilder attributes) {
        m_attributes = attributes;
        return this;
    }

    // ── Package-private getters for SDK internals ──

    boolean isEnabled() {
        return m_enabled;
    }

    PilotLogLevel getLogLevel() {
        return m_logLevel;
    }

    int getBatchSize() {
        return m_batchSize;
    }

    int getBufferSize() {
        return m_bufferSize;
    }

    @NonNull
    PilotLogAttributeBuilder getAttributes() {
        return m_attributes;
    }
}
