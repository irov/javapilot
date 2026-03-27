package org.pilot.sdk;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * A single metric sample with a type, value, and timestamp.
 */
public final class PilotMetricEntry {
    private final PilotMetricType m_type;
    private final double m_value;
    private final long m_timestampMs;

    public PilotMetricEntry(@NonNull PilotMetricType type, double value) {
        m_type = type;
        m_value = value;
        m_timestampMs = System.currentTimeMillis();
    }

    public PilotMetricEntry(@NonNull PilotMetricType type, double value, long timestampMs) {
        m_type = type;
        m_value = value;
        m_timestampMs = timestampMs;
    }

    @NonNull
    public PilotMetricType getType() {
        return m_type;
    }

    @NonNull
    public String getMetricType() {
        return m_type.getKey();
    }

    public double getValue() {
        return m_value;
    }

    public long getTimestampMs() {
        return m_timestampMs;
    }

    @NonNull
    JSONObject toJson() {
        JSONObject json = new JSONObject();
        try {
            json.put("metric_type", m_type.getKey());
            json.put("value", m_value);
            json.put("client_timestamp", m_timestampMs);
            json.put("aggregation", m_type.getAggregation().getKey());
        } catch (JSONException ignored) {
        }
        return json;
    }
}
