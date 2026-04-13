package org.pilot.sdk;

import androidx.annotation.NonNull;

/**
 * Typed action for {@link PilotSwitch} change events.
 * Provides a strongly-typed {@link #getValue()} instead of raw payload access.
 */
public final class PilotSwitchAction {
    private final PilotAction m_action;
    private final boolean m_value;

    PilotSwitchAction(@NonNull PilotAction action) {
        m_action = action;
        m_value = action.getPayload() != null && action.getPayload().optBoolean("value", false);
    }

    @NonNull
    public PilotAction getAction() {
        return m_action;
    }

    public boolean getValue() {
        return m_value;
    }
}
