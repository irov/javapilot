package org.pilot.sdk;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Label widget. Displays text with an optional color indicator.
 *
 * <p>Use {@link #textProvider} for automatic updates:</p>
 * <pre>{@code
 * root.addLabel("label-status", "Idle")
 *     .color("info")
 *     .textProvider(() -> game.getStatus());
 * }</pre>
 */
public final class PilotLabel extends PilotWidget {
    PilotLabel(@NonNull PilotUI ui, @NonNull String id, @NonNull String text) {
        super(ui, "label", id);
        put("text", text);
    }

    @NonNull
    public PilotLabel text(@NonNull String text) {
        put("text", text);
        return this;
    }

    @NonNull
    public PilotLabel color(@NonNull String color) {
        put("color", color);
        return this;
    }

    /**
     * Set a provider that returns the current text on each poll cycle.
     * The SDK will compare with the cached value and only send an update if changed.
     */
    @NonNull
    public PilotLabel textProvider(@Nullable PilotValueProvider provider) {
        setProvider("text", provider);
        return this;
    }
}
