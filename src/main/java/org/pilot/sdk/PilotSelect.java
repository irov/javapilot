package org.pilot.sdk;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Select (dropdown) widget. Triggers a "change" action with payload {@code {"value": "..."}}.
 */
public final class PilotSelect extends PilotWidget<PilotSelect> {
    PilotSelect(@NonNull PilotUI ui, @NonNull String label) {
        super(ui, "select");
        put("label", label);
    }

    /**
     * Set available options. Each element is {@code {value, label}}.
     */
    @NonNull
    public PilotSelect options(@NonNull String[][] options) {
        JSONArray arr = new JSONArray();
        for (String[] opt : options) {
            JSONObject o = new JSONObject();
            try {
                o.put("value", opt[0]);
                o.put("label", opt[1]);
            } catch (JSONException ignored) {
            }
            arr.put(o);
        }
        put("options", arr);
        return this;
    }

    @NonNull
    public PilotSelect defaultValue(@NonNull String value) {
        put("defaultValue", value);
        return this;
    }

    @NonNull
    public PilotSelect onChange(@Nullable PilotWidgetCallback callback) {
        m_ui.registerCallback(m_internalId, callback);
        return this;
    }
}
