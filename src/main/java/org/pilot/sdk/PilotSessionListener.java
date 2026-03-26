package org.pilot.sdk;

import androidx.annotation.NonNull;

/**
 * Listener for session lifecycle events.
 * All methods are called on a background thread.
 */
public interface PilotSessionListener {
    /** SDK is connecting to the server. */
    default void onPilotSessionConnecting() {}

    /** Connection request sent, waiting for dashboard approval. */
    default void onPilotSessionWaitingApproval(@NonNull String requestId) {}

    /** Session approved and active. Ready to send panel/logs and receive actions. */
    default void onPilotSessionStarted(@NonNull String sessionToken) {}

    /** Session closed (either by client or server). */
    default void onPilotSessionClosed() {}

    /** Connection request was rejected by a dashboard user. */
    default void onPilotSessionRejected() {}

    /** Authentication failed (HTTP 401). The API token is invalid or expired. */
    default void onPilotSessionAuthFailed() {}

    /** An error occurred. The session may still be running (transient errors). */
    default void onPilotSessionError(@NonNull PilotException exception) {}
}
