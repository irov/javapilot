package org.pilot.sdk;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Listener for Pilot SDK internal diagnostic logs.
 *
 * <p>This callback receives client-side SDK logs and is separate from the log
 * pipeline that buffers and sends application logs to the Pilot server.</p>
 *
 * <pre>{@code
 * PilotLogConfigBuilder logConfig = new PilotLogConfigBuilder()
 *     .setLoggerListener((level, tag, message, throwable) -> {
 *         MyEngine.log(tag + ": " + message);
 *     });
 * }</pre>
 */
public interface PilotLoggerListener {
    /**
     * Called for each internal SDK log message.
     *
     * @param level     Log level
     * @param tag       Log tag (always "PilotSDK")
     * @param message   Formatted log message
     * @param throwable Optional throwable (may be null)
     */
    void onPilotLoggerMessage(@NonNull PilotLogLevel level, @NonNull String tag, @NonNull String message, @Nullable Throwable throwable);
}