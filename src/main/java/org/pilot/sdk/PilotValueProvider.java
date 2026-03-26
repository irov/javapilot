package org.pilot.sdk;

import androidx.annotation.NonNull;

/**
 * Provides a dynamic value for a widget property.
 * Called on the SDK poll thread — keep implementations lightweight.
 *
 * <pre>{@code
 * root.addStat("FPS")
 *     .unit("fps")
 *     .valueProvider(() -> game.getFps());
 * }</pre>
 */
@FunctionalInterface
public interface PilotValueProvider {
    @NonNull
    Object getValue();
}
