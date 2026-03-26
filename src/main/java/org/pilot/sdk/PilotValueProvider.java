package org.pilot.sdk;

import androidx.annotation.Nullable;

/**
 * Provides a dynamic value for a widget property or attribute.
 * Called on the SDK poll thread — keep implementations lightweight.
 */
@FunctionalInterface
public interface PilotValueProvider {
    @Nullable
    Object getValue();
}
