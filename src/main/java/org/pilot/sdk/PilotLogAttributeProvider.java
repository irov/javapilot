package org.pilot.sdk;

import androidx.annotation.Nullable;

/**
 * Provides a dynamic attribute value for log entries.
 * Called on each log flush to resolve the current value.
 *
 * <pre>{@code
 * Pilot.addLogAttribute("scene", () -> getCurrentSceneName());
 * }</pre>
 */
public interface PilotLogAttributeProvider {
    /**
     * @return The current value for this attribute, or null to omit.
     */
    @Nullable
    String resolve();
}
