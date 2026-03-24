package org.pilot.sdk;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Button widget. Triggers a "click" action when pressed on the dashboard.
 *
 * <pre>{@code
 * layout.addButton("btn-restart", "Restart")
 *     .variant("contained").color("error")
 *     .onClick(action -> restartGame());
 * }</pre>
 */
public final class PilotButton extends PilotWidget {
    PilotButton(@NonNull PilotUI ui, @NonNull String id, @NonNull String label) {
        super(ui, "button", id);
        put("label", label);
    }

    @NonNull
    public PilotButton variant(@NonNull String variant) {
        put("variant", variant);
        return this;
    }

    @NonNull
    public PilotButton color(@NonNull String color) {
        put("color", color);
        return this;
    }

    @NonNull
    public PilotButton disabled(boolean disabled) {
        put("disabled", disabled);
        return this;
    }

    @NonNull
    public PilotButton onClick(@Nullable PilotWidgetCallback callback) {
        m_ui.registerCallback(m_id, callback);
        return this;
    }
}
