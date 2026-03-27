package org.pilot.sdk;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Configuration for {@link Pilot} initialization.
 */
public final class PilotConfig {
    final String baseUrl;
    final String apiToken;
    final String deviceId;
    final String deviceName;
    final long pollIntervalMs;
    final long heartbeatIntervalMs;
    final long actionPollIntervalMs;
    final long logFlushIntervalMs;
    final PilotLogLevel logLevel;
    final PilotLogger logger;
    final boolean autoConnect;
    final PilotSessionListener sessionListener;
    final PilotActionListener actionListener;
    final PilotSessionAttributeBuilder sessionAttributes;
    final PilotLogAttributeBuilder logAttributes;
    final int logBatchSize;
    final int logBufferSize;
    final PilotMetricConfigBuilder metricConfig;

    private PilotConfig(Builder builder) {
        this.baseUrl = builder.baseUrl;
        this.apiToken = builder.apiToken;
        this.deviceId = builder.deviceId;
        this.deviceName = builder.deviceName;
        this.pollIntervalMs = builder.pollIntervalMs;
        this.heartbeatIntervalMs = builder.heartbeatIntervalMs;
        this.actionPollIntervalMs = builder.actionPollIntervalMs;
        this.logFlushIntervalMs = builder.logFlushIntervalMs;
        this.logLevel = builder.logLevel;
        this.logger = builder.logger;
        this.autoConnect = builder.autoConnect;
        this.sessionListener = builder.sessionListener;
        this.actionListener = builder.actionListener;
        this.sessionAttributes = builder.sessionAttributes;
        this.logAttributes = builder.logAttributes;
        this.logBatchSize = builder.logBatchSize;
        this.logBufferSize = builder.logBufferSize;
        this.metricConfig = builder.metricConfig;
    }

    @NonNull
    public String getBaseUrl() {
        return baseUrl;
    }

    @NonNull
    public String getApiToken() {
        return apiToken;
    }

    @Nullable
    public String getDeviceId() {
        return deviceId;
    }

    @Nullable
    public String getDeviceName() {
        return deviceName;
    }

    public static class Builder {
        private String baseUrl;
        private String apiToken;
        private String deviceId;
        private String deviceName;
        private long pollIntervalMs = 10000;
        private long heartbeatIntervalMs = 60000;
        private long actionPollIntervalMs = 2000;
        private long logFlushIntervalMs = 5000;
        private PilotLogLevel logLevel = PilotLogLevel.INFO;
        private PilotLogger logger = null;
        private boolean autoConnect = true;
        private PilotSessionListener sessionListener = null;
        private PilotActionListener actionListener = null;
        private PilotSessionAttributeBuilder sessionAttributes = new PilotSessionAttributeBuilder();
        private PilotLogAttributeBuilder logAttributes = new PilotLogAttributeBuilder();
        private int logBatchSize = 100;
        private int logBufferSize = 1000;
        private PilotMetricConfigBuilder metricConfig = new PilotMetricConfigBuilder();

        /**
         * @param baseUrl  Pilot server URL, e.g. "https://pilot.example.com"
         * @param apiToken Project API token starting with "plt_"
         */
        public Builder(@NonNull String baseUrl, @NonNull String apiToken) {
            this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
            this.apiToken = apiToken;
        }

        public Builder setDeviceId(@NonNull String deviceId) {
            this.deviceId = deviceId;
            return this;
        }

        public Builder setDeviceName(@NonNull String deviceName) {
            this.deviceName = deviceName;
            return this;
        }

        public Builder setPollIntervalMs(long ms) {
            this.pollIntervalMs = ms;
            return this;
        }

        public Builder setHeartbeatIntervalMs(long ms) {
            this.heartbeatIntervalMs = ms;
            return this;
        }

        public Builder setActionPollIntervalMs(long ms) {
            this.actionPollIntervalMs = ms;
            return this;
        }

        public Builder setLogFlushIntervalMs(long ms) {
            this.logFlushIntervalMs = ms;
            return this;
        }

        public Builder setLogLevel(@NonNull PilotLogLevel level) {
            this.logLevel = level;
            return this;
        }

        /**
         * Set a custom logger to redirect all SDK log output.
         * If not set, logs go to android.util.Log.
         */
        public Builder setLogger(@Nullable PilotLogger logger) {
            this.logger = logger;
            return this;
        }

        /**
         * Whether to connect automatically on {@link Pilot#initialize}.
         * If false, {@link Pilot#connect()} must be called explicitly.
         * Default: true.
         */
        public Builder setAutoConnect(boolean autoConnect) {
            this.autoConnect = autoConnect;
            return this;
        }

        /**
         * Set a listener for session lifecycle events (connecting, approved, error, etc.).
         */
        public Builder setSessionListener(@Nullable PilotSessionListener listener) {
            this.sessionListener = listener;
            return this;
        }

        /**
         * Set a listener for actions received from the dashboard.
         */
        public Builder setActionListener(@Nullable PilotActionListener listener) {
            this.actionListener = listener;
            return this;
        }

        /**
         * Set session attributes (static and dynamic) via a builder.
         */
        public Builder setSessionAttributes(@NonNull PilotSessionAttributeBuilder builder) {
            this.sessionAttributes = builder;
            return this;
        }

        /**
         * Set log attributes (static and dynamic) via a builder.
         * Static values are fixed; dynamic providers are resolved at each log() call.
         */
        public Builder setLogAttributes(@NonNull PilotLogAttributeBuilder builder) {
            this.logAttributes = builder;
            return this;
        }

        public Builder setLogBatchSize(int size) {
            this.logBatchSize = size;
            return this;
        }

        public Builder setLogBufferSize(int size) {
            this.logBufferSize = size;
            return this;
        }

        /**
         * Set the metric configuration via a builder.
         *
         * <pre>{@code
         * PilotMetricConfigBuilder metricConfig = new PilotMetricConfigBuilder()
         *     .setEnabled(true)
         *     .setSampleIntervalMs(200)
         *     .setFlushIntervalMs(5000)
         *     .addCollector(out -> {
         *         out.add(new PilotMetricEntry(PilotMetricType.DRAW_CALLS, renderer.getDrawCalls()));
         *     });
         *
         * builder.setMetricConfig(metricConfig);
         * }</pre>
         */
        public Builder setMetricConfig(@NonNull PilotMetricConfigBuilder config) {
            this.metricConfig = config;
            return this;
        }

        public PilotConfig build() {
            if (baseUrl == null || baseUrl.isEmpty()) {
                throw new IllegalArgumentException("baseUrl is required");
            }
            if (apiToken == null || apiToken.isEmpty()) {
                throw new IllegalArgumentException("apiToken is required");
            }
            return new PilotConfig(this);
        }
    }
}
