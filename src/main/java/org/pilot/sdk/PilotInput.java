package org.pilot.sdk;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Text input widget. Triggers a "change" action with payload {@code {"value": "..."}}.
 */
public final class PilotInput extends PilotWidget {
    PilotInput(@NonNull PilotUI ui, @NonNull String id, @NonNull String label) {
        super(ui, "input", id);
        put("label", label);
    }

    @NonNull
    public PilotInput inputType(@NonNull String type) {
        put("inputType", type);
        return this;
    }

    @NonNull
    public PilotInput defaultValue(@NonNull String value) {
        put("defaultValue", value);
        return this;
    }

    @NonNull
    public PilotInput placeholder(@NonNull String placeholder) {
        put("placeholder", placeholder);
        return this;
    }

    @NonNull
    public PilotInput onSubmit(@Nullable PilotWidgetCallback callback) {
        m_ui.registerCallback(m_id, callback);
        return this;
    }
}
