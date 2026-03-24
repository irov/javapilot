package org.pilot.sdk;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Stat widget. Displays a numeric value with a label and unit.
 *
 * <p>Use {@link #valueProvider} for automatic updates:</p>
 * <pre>{@code
 * root.addStat("stat-fps", "FPS")
 *     .unit("fps")
 *     .valueProvider(() -> String.valueOf(game.getFps()));
 * }</pre>
 */
public final class PilotStat extends PilotWidget {
    PilotStat(@NonNull PilotUI ui, @NonNull String id, @NonNull String label) {
        super(ui, "stat", id);
        put("label", label);
    }

    @NonNull
    public PilotStat value(@NonNull String value) {
        put("value", value);
        return this;
    }

    @NonNull
    public PilotStat unit(@NonNull String unit) {
        put("unit", unit);
        return this;
    }

    /**
     * Set a provider that returns the current value on each poll cycle.
     * The SDK will compare with the cached value and only send an update if changed.
     */
    @NonNull
    public PilotStat valueProvider(@Nullable PilotValueProvider provider) {
        setProvider("value", provider);
        return this;
    }
}
