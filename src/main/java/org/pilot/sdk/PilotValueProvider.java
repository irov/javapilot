package org.pilot.sdk;

import androidx.annotation.NonNull;

/**
 * Provides a dynamic value for a widget property.
 * Called on the SDK poll thread — keep implementations lightweight.
 *
 * <pre>{@code
 * root.addStat("stat-fps", "FPS")
 *     .unit("fps")
 *     .valueProvider(() -> String.valueOf(game.getFps()));
 * }</pre>
 */
@FunctionalInterface
public interface PilotValueProvider {
    @NonNull
    String getValue();
}
