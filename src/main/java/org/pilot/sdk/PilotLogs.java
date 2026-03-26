package org.pilot.sdk;

import androidx.annotation.NonNull;

/**
 * Logs display widget. Shows recent log output.
 */
public final class PilotLogs extends PilotWidget<PilotLogs> {
    PilotLogs(@NonNull PilotUI ui, @NonNull String label) {
        super(ui, "logs");
        put("label", label);
    }

    @NonNull
    public PilotLogs maxLines(int maxLines) {
        put("maxLines", maxLines);
        return this;
    }
}
