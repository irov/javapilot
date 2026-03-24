package org.pilot.sdk;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Interface for redirecting Pilot SDK internal logs to a custom logging system.
 *
 * <p>Implement this to route SDK logs through your engine's logger (e.g. Mengine, Unity, etc.).</p>
 *
 * <pre>{@code
 * Pilot.initialize(new PilotConfig.Builder(url, token)
 *     .setLogger((level, tag, message, throwable) -> {
 *         MyEngine.log(level.getValue(), tag + ": " + message);
 *     })
 *     .build());
 * }</pre>
 */
public interface PilotLogger {
    /**
     * Called for each SDK log message.
     *
     * @param level     Log level
     * @param tag       Log tag (always "PilotSDK")
     * @param message   Formatted log message
     * @param throwable Optional throwable (may be null)
     */
    void log(@NonNull PilotLogLevel level, @NonNull String tag, @NonNull String message, @Nullable Throwable throwable);
}
