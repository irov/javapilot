package org.pilot.sdk;

import androidx.annotation.NonNull;

/**
 * Logs display widget. Shows recent log output.
 */
public final class PilotLogs extends PilotWidget {
    PilotLogs(@NonNull PilotUI ui, @NonNull String id, @NonNull String label) {
        super(ui, "logs", id);
        put("label", label);
    }

    @NonNull
    public PilotLogs maxLines(int maxLines) {
        put("maxLines", maxLines);
        return this;
    }
}
