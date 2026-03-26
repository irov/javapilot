package org.pilot.sdk;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Switch (toggle) widget. Triggers a "change" action with payload {@code {"value": true/false}}.
 */
public final class PilotSwitch extends PilotWidget<PilotSwitch> {
    PilotSwitch(@NonNull PilotUI ui, @NonNull String label) {
        super(ui, "switch");
        put("label", label);
    }

    @NonNull
    public PilotSwitch defaultValue(boolean value) {
        put("defaultValue", value);
        return this;
    }

    @NonNull
    public PilotSwitch onChange(@Nullable PilotWidgetCallback callback) {
        m_ui.registerCallback(m_internalId, callback);
        return this;
    }
}
