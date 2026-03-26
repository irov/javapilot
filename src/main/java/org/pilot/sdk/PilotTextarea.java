package org.pilot.sdk;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Multi-line text input widget.
 */
public final class PilotTextarea extends PilotWidget<PilotTextarea> {
    PilotTextarea(@NonNull PilotUI ui, @NonNull String label) {
        super(ui, "textarea");
        put("label", label);
    }

    @NonNull
    public PilotTextarea rows(int rows) {
        put("rows", rows);
        return this;
    }

    @NonNull
    public PilotTextarea defaultValue(@NonNull String value) {
        put("defaultValue", value);
        return this;
    }

    @NonNull
    public PilotTextarea onSubmit(@Nullable PilotWidgetCallback callback) {
        m_ui.registerCallback(m_internalId, callback);
        return this;
    }
}
