package org.pilot.sdk;

import androidx.annotation.NonNull;

/**
 * Listener for session lifecycle events.
 * All methods are called on a background thread.
 */
public interface PilotSessionListener {
    /** SDK is connecting to the server. */
    default void onConnecting() {}

    /** Connection request sent, waiting for dashboard approval. */
    default void onWaitingApproval(@NonNull String requestId) {}

    /** Session approved and active. Ready to send panel/logs and receive actions. */
    default void onSessionStarted(@NonNull String sessionToken) {}

    /** Session closed (either by client or server). */
    default void onSessionClosed() {}

    /** Connection request was rejected by a dashboard user. */
    default void onRejected() {}

    /** An error occurred. The session may still be running (transient errors). */
    default void onError(@NonNull PilotException exception) {}
}
