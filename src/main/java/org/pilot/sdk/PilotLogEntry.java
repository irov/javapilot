package org.pilot.sdk;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/**
 * A single log entry to be sent to the Pilot server.
 */
public final class PilotLogEntry {
    private static final SimpleDateFormat ISO_FORMAT;

    static {
        ISO_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        ISO_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    private final String m_level;
    private final String m_message;
    private final String m_category;
    private final String m_thread;
    private final Map<String, Object> m_metadata;
    private final JSONObject m_attributes;
    private final String m_clientTimestamp;

    public PilotLogEntry(@NonNull PilotLogLevel level, @NonNull String message, @Nullable Map<String, Object> metadata) {
        this(level, message, null, null, metadata, null);
    }

    public PilotLogEntry(@NonNull PilotLogLevel level, @NonNull String message,
                         @Nullable String category, @Nullable String thread,
                         @Nullable Map<String, Object> metadata) {
        this(level, message, category, thread, metadata, null);
    }

    public PilotLogEntry(@NonNull PilotLogLevel level, @NonNull String message,
                         @Nullable String category, @Nullable String thread,
                         @Nullable Map<String, Object> metadata, @Nullable JSONObject attributes) {
        m_level = level.getValue();
        m_message = message;
        m_category = category;
        m_thread = thread;
        m_metadata = metadata;
        m_attributes = attributes;

        synchronized (ISO_FORMAT) {
            m_clientTimestamp = ISO_FORMAT.format(new Date());
        }
    }

    public PilotLogEntry(@NonNull String level, @NonNull String message, @Nullable Map<String, Object> metadata) {
        this(level, message, null, null, metadata, null);
    }

    public PilotLogEntry(@NonNull String level, @NonNull String message,
                         @Nullable String category, @Nullable String thread,
                         @Nullable Map<String, Object> metadata) {
        this(level, message, category, thread, metadata, null);
    }

    public PilotLogEntry(@NonNull String level, @NonNull String message,
                         @Nullable String category, @Nullable String thread,
                         @Nullable Map<String, Object> metadata, @Nullable JSONObject attributes) {
        m_level = level;
        m_message = message;
        m_category = category;
        m_thread = thread;
        m_attributes = attributes;
        m_metadata = metadata;

        synchronized (ISO_FORMAT) {
            m_clientTimestamp = ISO_FORMAT.format(new Date());
        }
    }

    public static PilotLogEntry debug(@NonNull String message) {
        return new PilotLogEntry(PilotLogLevel.DEBUG, message, (Map<String, Object>) null);
    }

    public static PilotLogEntry info(@NonNull String message) {
        return new PilotLogEntry(PilotLogLevel.INFO, message, (Map<String, Object>) null);
    }

    public static PilotLogEntry warning(@NonNull String message) {
        return new PilotLogEntry(PilotLogLevel.WARNING, message, (Map<String, Object>) null);
    }

    public static PilotLogEntry error(@NonNull String message) {
        return new PilotLogEntry(PilotLogLevel.ERROR, message, (Map<String, Object>) null);
    }

    public static PilotLogEntry critical(@NonNull String message) {
        return new PilotLogEntry(PilotLogLevel.CRITICAL, message, (Map<String, Object>) null);
    }

    @NonNull
    public String getLevel() {
        return m_level;
    }

    @NonNull
    public String getMessage() {
        return m_message;
    }

    @NonNull
    JSONObject toJson() {
        JSONObject json = new JSONObject();
        try {
            json.put("level", m_level);
            json.put("message", m_message);
            json.put("client_timestamp", m_clientTimestamp);

            if (m_category != null) {
                json.put("category", m_category);
            }

            if (m_thread != null) {
                json.put("thread", m_thread);
            }

            if (m_metadata != null && !m_metadata.isEmpty()) {
                JSONObject metaJson = new JSONObject();
                for (Map.Entry<String, Object> entry : m_metadata.entrySet()) {
                    Object value = entry.getValue();
                    metaJson.put(entry.getKey(), value != null ? value : JSONObject.NULL);
                }
                json.put("metadata", metaJson);
            }

            if (m_attributes != null) {
                json.put("attributes", m_attributes);
            }
        } catch (JSONException ignored) {
        }

        return json;
    }
}
