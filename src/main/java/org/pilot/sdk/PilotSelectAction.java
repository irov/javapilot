package org.pilot.sdk;

import androidx.annotation.NonNull;

/**
 * Typed action for {@link PilotSelect} change events.
 * Provides a strongly-typed {@link #getValue()} instead of raw payload access.
 */
public final class PilotSelectAction {
    private final PilotAction m_action;
    private final String m_value;

    PilotSelectAction(@NonNull PilotAction action) {
        m_action = action;
        m_value = action.getPayload() != null ? action.getPayload().optString("value", "") : "";
    }

    @NonNull
    public PilotAction getAction() {
        return m_action;
    }

    @NonNull
    public String getValue() {
        return m_value;
    }
}
