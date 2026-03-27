package org.pilot.sdk;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Low-level HTTP client for the Pilot server API.
 * Thread-safe — can be shared across threads.
 */
final class PilotHttpClient {
    private static final MediaType JSON_MEDIA = MediaType.get("application/json; charset=utf-8");
    private static final RequestBody EMPTY_BODY = RequestBody.create("", JSON_MEDIA);

    private final String m_baseUrl;
    private final String m_apiToken;
    private final OkHttpClient m_http;

    PilotHttpClient(@NonNull String baseUrl, @NonNull String apiToken) {
        m_baseUrl = baseUrl;
        m_apiToken = apiToken;
        m_http = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .build();
    }

    void shutdown() {
        m_http.dispatcher().executorService().shutdown();
        m_http.connectionPool().evictAll();
    }

    // ── Client endpoints ──

    PilotConnectResponse connect(@NonNull String deviceId, @NonNull String deviceName,
                                @NonNull Map<String, Object> sessionAttributes) throws PilotException {
        JSONObject body = new JSONObject();
        try {
            body.put("device_id", deviceId);
            body.put("device_name", deviceName);
            if (!sessionAttributes.isEmpty()) {
                JSONObject attrs = new JSONObject();
                for (Map.Entry<String, Object> entry : sessionAttributes.entrySet()) {
                    attrs.put(entry.getKey(), entry.getValue());
                }
                body.put("session_attributes", attrs);
            }
        } catch (JSONException e) {
            throw new PilotException("Failed to build connect request", e);
        }

        Request request = apiTokenRequest("/api/client/connect")
                .post(jsonBody(body))
                .build();

        return PilotConnectResponse.fromJson(execute(request));
    }

    PilotConnectResponse pollStatus(@NonNull String requestId) throws PilotException {
        Request request = apiTokenRequest("/api/client/poll-status/" + requestId)
                .get()
                .build();

        return PilotConnectResponse.fromJson(execute(request));
    }

    boolean heartbeat(@NonNull String sessionToken,
                      @Nullable Map<String, Object> changedAttributes) throws PilotException {
        JSONObject body = new JSONObject();
        try {
            if (changedAttributes != null && !changedAttributes.isEmpty()) {
                JSONObject attrs = new JSONObject();
                for (Map.Entry<String, Object> entry : changedAttributes.entrySet()) {
                    attrs.put(entry.getKey(), entry.getValue());
                }
                body.put("session_attributes", attrs);
            }
        } catch (JSONException e) {
            throw new PilotException("Failed to build heartbeat request", e);
        }

        Request request = sessionTokenRequest("/api/client/session/heartbeat", sessionToken)
                .post(body.length() > 0 ? jsonBody(body) : EMPTY_BODY)
                .build();

        return execute(request).optBoolean("ok", false);
    }

    boolean closeSession(@NonNull String sessionToken) throws PilotException {
        Request request = sessionTokenRequest("/api/client/session/close", sessionToken)
                .post(EMPTY_BODY)
                .build();

        return execute(request).optBoolean("ok", false);
    }

    JSONObject submitPanel(@NonNull String sessionToken, @NonNull JSONObject layout) throws PilotException {
        JSONObject body = new JSONObject();
        try {
            body.put("layout", layout);
        } catch (JSONException e) {
            throw new PilotException("Failed to build panel request", e);
        }

        Request request = sessionTokenRequest("/api/client/session/panel", sessionToken)
                .post(jsonBody(body))
                .build();

        return execute(request);
    }

    JSONObject pollActions(@NonNull String sessionToken,
                           @Nullable Map<String, Object> changedAttributes) throws PilotException {
        JSONObject body = new JSONObject();
        try {
            if (changedAttributes != null && !changedAttributes.isEmpty()) {
                JSONObject attrs = new JSONObject();
                for (Map.Entry<String, Object> entry : changedAttributes.entrySet()) {
                    attrs.put(entry.getKey(), entry.getValue());
                }
                body.put("session_attributes", attrs);
            }
        } catch (JSONException e) {
            throw new PilotException("Failed to build action poll request", e);
        }

        Request request = sessionTokenRequest("/api/client/session/actions/poll", sessionToken)
                .post(body.length() > 0 ? jsonBody(body) : EMPTY_BODY)
                .build();

        return execute(request);
    }

    void acknowledgeAction(@NonNull String sessionToken, @NonNull String actionId, @Nullable JSONObject ackPayload) throws PilotException {
        JSONObject body = new JSONObject();
        try {
            body.put("action_id", actionId);
            body.put("ack_payload", ackPayload != null ? ackPayload : new JSONObject());
        } catch (JSONException e) {
            throw new PilotException("Failed to build ack request", e);
        }

        Request request = sessionTokenRequest("/api/client/session/actions/ack", sessionToken)
                .post(jsonBody(body))
                .build();

        execute(request);
    }


    void sendLogs(@NonNull String sessionToken, @NonNull List<PilotLogEntry> logs) throws PilotException {
        if (logs.isEmpty()) {
            return;
        }

        JSONArray logsArray = new JSONArray();
        for (PilotLogEntry entry : logs) {
            logsArray.put(entry.toJson());
        }

        JSONObject body = new JSONObject();
        try {
            body.put("logs", logsArray);
        } catch (JSONException e) {
            throw new PilotException("Failed to build logs request", e);
        }

        Request request = sessionTokenRequest("/api/client/session/logs", sessionToken)
                .post(jsonBody(body))
                .build();

        execute(request);
    }

    void sendMetrics(@NonNull String sessionToken, @NonNull List<PilotMetricEntry> metrics) throws PilotException {
        if (metrics.isEmpty()) {
            return;
        }

        JSONArray metricsArray = new JSONArray();
        for (PilotMetricEntry entry : metrics) {
            metricsArray.put(entry.toJson());
        }

        JSONObject body = new JSONObject();
        try {
            body.put("metrics", metricsArray);
        } catch (JSONException e) {
            throw new PilotException("Failed to build metrics request", e);
        }

        Request request = sessionTokenRequest("/api/client/session/metrics", sessionToken)
                .post(jsonBody(body))
                .build();

        execute(request);
    }

    // ── Helpers ──

    private Request.Builder apiTokenRequest(@NonNull String path) {
        return new Request.Builder()
                .url(m_baseUrl + path)
                .header("X-Api-Token", m_apiToken);
    }

    private Request.Builder sessionTokenRequest(@NonNull String path, @NonNull String sessionToken) {
        return new Request.Builder()
                .url(m_baseUrl + path)
                .header("X-Session-Token", sessionToken);
    }

    private static RequestBody jsonBody(@NonNull JSONObject json) {
        return RequestBody.create(json.toString(), JSON_MEDIA);
    }

    private JSONObject execute(@NonNull Request request) throws PilotException {
        try (Response response = m_http.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";

            if (!response.isSuccessful()) {
                String detail = responseBody;
                try {
                    JSONObject errorJson = new JSONObject(responseBody);
                    detail = errorJson.optString("detail", responseBody);
                } catch (JSONException ignored) {
                }

                throw new PilotException(response.code(), "HTTP " + response.code() + ": " + detail);
            }

            if (responseBody.isEmpty()) {
                return new JSONObject();
            }

            return new JSONObject(responseBody);
        } catch (IOException e) {
            throw new PilotException("Network error: " + e.getMessage(), e);
        } catch (JSONException e) {
            throw new PilotException("Failed to parse server response", e);
        }
    }
}
