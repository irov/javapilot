package org.pilot.sdk;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONObject;

/**
 * Response from connect / poll-status endpoints.
 */
public final class PilotConnectResponse {
    private final String m_requestId;
    private final String m_status;
    private final String m_sessionToken;

    PilotConnectResponse(@NonNull String requestId, @NonNull String status, @Nullable String sessionToken) {
        m_requestId = requestId;
        m_status = status;
        m_sessionToken = sessionToken;
    }

    @NonNull
    public String getRequestId() {
        return m_requestId;
    }

    @NonNull
    public String getStatus() {
        return m_status;
    }

    @Nullable
    public String getSessionToken() {
        return m_sessionToken;
    }

    public boolean isPending() {
        return "pending".equals(m_status);
    }

    public boolean isApproved() {
        return "approved".equals(m_status);
    }

    public boolean isRejected() {
        return "rejected".equals(m_status);
    }

    static PilotConnectResponse fromJson(@NonNull JSONObject json) {
        String token = json.isNull("session_token") ? null : json.optString("session_token", null);
        return new PilotConnectResponse(
                json.optString("request_id", ""),
                json.optString("status", ""),
                token
        );
    }
}
