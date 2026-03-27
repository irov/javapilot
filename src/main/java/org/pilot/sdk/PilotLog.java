package org.pilot.sdk;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Internal SDK logger. Delegates to {@link PilotLoggerListener} if set, otherwise to android.util.Log.
 */
final class PilotLog {
    private static final String TAG = "PilotSDK";
    private static PilotLogLevel s_level = PilotLogLevel.INFO;
    private static volatile PilotLoggerListener s_loggerListener = null;

    static void setLevel(@NonNull PilotLogLevel level) {
        s_level = level;
    }

    static void setLoggerListener(@Nullable PilotLoggerListener loggerListener) {
        s_loggerListener = loggerListener;
    }

    static void d(@NonNull String message, Object... args) {
        if (s_level.ordinal() <= PilotLogLevel.DEBUG.ordinal()) {
            String msg = format(message, args);
            PilotLoggerListener loggerListener = s_loggerListener;
            if (loggerListener != null) {
                loggerListener.onPilotLoggerMessage(PilotLogLevel.DEBUG, TAG, msg, null);
            } else {
                Log.d(TAG, msg);
            }
        }
    }

    static void i(@NonNull String message, Object... args) {
        if (s_level.ordinal() <= PilotLogLevel.INFO.ordinal()) {
            String msg = format(message, args);
            PilotLoggerListener loggerListener = s_loggerListener;
            if (loggerListener != null) {
                loggerListener.onPilotLoggerMessage(PilotLogLevel.INFO, TAG, msg, null);
            } else {
                Log.i(TAG, msg);
            }
        }
    }

    static void w(@NonNull String message, Object... args) {
        if (s_level.ordinal() <= PilotLogLevel.WARNING.ordinal()) {
            String msg = format(message, args);
            PilotLoggerListener loggerListener = s_loggerListener;
            if (loggerListener != null) {
                loggerListener.onPilotLoggerMessage(PilotLogLevel.WARNING, TAG, msg, null);
            } else {
                Log.w(TAG, msg);
            }
        }
    }

    static void e(@NonNull String message, Object... args) {
        if (s_level.ordinal() <= PilotLogLevel.ERROR.ordinal()) {
            String msg = format(message, args);
            PilotLoggerListener loggerListener = s_loggerListener;
            if (loggerListener != null) {
                loggerListener.onPilotLoggerMessage(PilotLogLevel.ERROR, TAG, msg, null);
            } else {
                Log.e(TAG, msg);
            }
        }
    }

    static void e(@NonNull String message, @NonNull Throwable t) {
        PilotLoggerListener loggerListener = s_loggerListener;
        if (loggerListener != null) {
            loggerListener.onPilotLoggerMessage(PilotLogLevel.EXCEPTION, TAG, message, t);
        } else {
            Log.e(TAG, message, t);
        }
    }

    private static String format(@NonNull String message, Object... args) {
        if (args.length == 0) {
            return message;
        }
        return String.format(message, args);
    }
}
