package org.pilot.sdk;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * A tab in the Pilot dashboard UI.
 * Each service/module adds its own tab via {@link PilotUI#addTab}.
 * Call {@link #vertical()} or {@link #horizontal()} to set the root layout direction.
 */
public final class PilotTab {
    private final PilotUI m_ui;
    private final String m_id;
    private final String m_title;
    private PilotLayout m_layout;

    PilotTab(@NonNull PilotUI ui, @NonNull String id, @NonNull String title) {
        m_ui = ui;
        m_id = id;
        m_title = title;
    }

    @NonNull
    public String getId() {
        return m_id;
    }

    @NonNull
    public String getTitle() {
        return m_title;
    }

    /**
     * Get or create the root layout.
     */
    @Nullable
    public PilotLayout getLayout() {
        return m_layout;
    }

    /**
     * Set the root layout to vertical and return it for building.
     */
    @NonNull
    public PilotLayout vertical() {
        m_layout = new PilotLayout(m_ui, PilotLayout.Direction.VERTICAL);
        return m_layout;
    }

    /**
     * Set the root layout to horizontal and return it for building.
     */
    @NonNull
    public PilotLayout horizontal() {
        m_layout = new PilotLayout(m_ui, PilotLayout.Direction.HORIZONTAL);
        return m_layout;
    }

    @NonNull
    JSONObject toJson() {
        JSONObject json = new JSONObject();
        try {
            json.put("id", m_id);
            json.put("title", m_title);
            if (m_layout != null) {
                json.put("layout", m_layout.toJson());
            }
        } catch (JSONException ignored) {
        }
        return json;
    }
}
