package org.pilot.sdk;

import androidx.annotation.NonNull;

/**
 * Callback for widget actions from the Pilot dashboard (button click, switch toggle, etc.).
 * Registered per-widget via {@link PilotButton#onClick}, {@link PilotSwitch#onChange}, etc.
 */
@FunctionalInterface
public interface PilotWidgetCallback {
    void onPilotWidgetAction(@NonNull PilotAction action);
}
