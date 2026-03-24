package org.pilot.sdk;

import androidx.annotation.NonNull;

/**
 * Listener for actions dispatched from the Pilot dashboard.
 */
public interface PilotActionListener {
    /**
     * Called when an action is received from the dashboard (e.g. button click).
     * Called on a background thread.
     *
     * @param action The action to handle
     */
    void onAction(@NonNull PilotAction action);
}
