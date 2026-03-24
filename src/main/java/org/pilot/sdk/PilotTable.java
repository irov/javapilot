package org.pilot.sdk;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * Table widget. Displays data in rows and columns.
 */
public final class PilotTable extends PilotWidget {
    PilotTable(@NonNull PilotUI ui, @NonNull String id, @NonNull String label) {
        super(ui, "table", id);
        put("label", label);
    }

    /**
     * Set table columns. Each element is {@code {key, label}}.
     */
    @NonNull
    public PilotTable columns(@NonNull String[][] columns) {
        JSONArray arr = new JSONArray();
        for (String[] col : columns) {
            JSONObject o = new JSONObject();
            try {
                o.put("key", col[0]);
                o.put("label", col[1]);
            } catch (JSONException ignored) {
            }
            arr.put(o);
        }
        put("columns", arr);
        return this;
    }

    /**
     * Set table row data.
     */
    @NonNull
    public PilotTable rows(@NonNull List<JSONObject> rows) {
        JSONArray arr = new JSONArray();
        for (JSONObject row : rows) {
            arr.put(row);
        }
        put("rows", arr);
        return this;
    }
}
