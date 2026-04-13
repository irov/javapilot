package org.pilot.sdk;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.function.Consumer;

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

    @NonNull
    public PilotSwitch onChange(@NonNull Consumer<PilotSwitchAction> callback) {
        m_ui.registerCallback(m_internalId, action -> callback.accept(new PilotSwitchAction(action)));
        return this;
    }
}
