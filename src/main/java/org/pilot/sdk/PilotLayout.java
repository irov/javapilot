package org.pilot.sdk;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * A layout container for Pilot UI widgets.
 * Supports vertical/horizontal arrangement and nesting, similar to LayoutBox.
 *
 * <pre>{@code
 * PilotLayout root = tab.vertical();
 *
 * root.addButton("btn-1", "Click Me").onClick(action -> { ... });
 * root.addStat("stat-fps", "FPS").value("60").unit("fps");
 *
 * PilotLayout row = root.addHorizontal();
 * row.addButton("btn-a", "A").onClick(action -> doA());
 * row.addPadding(1.0);
 * row.addButton("btn-b", "B").onClick(action -> doB());
 * }</pre>
 */
public final class PilotLayout {

    public enum Direction {
        VERTICAL("vertical"),
        HORIZONTAL("horizontal");

        private final String m_value;

        Direction(String value) {
            m_value = value;
        }

        String getValue() {
            return m_value;
        }
    }

    private final PilotUI m_ui;
    private final Direction m_direction;
    private final List<Object> m_children = new ArrayList<>();

    PilotLayout(@NonNull PilotUI ui, @NonNull Direction direction) {
        m_ui = ui;
        m_direction = direction;
    }

    @NonNull
    public Direction getDirection() {
        return m_direction;
    }

    // ── Sub-layouts ──

    @NonNull
    public PilotLayout addVertical() {
        PilotLayout sub = new PilotLayout(m_ui, Direction.VERTICAL);
        m_children.add(sub);
        return sub;
    }

    @NonNull
    public PilotLayout addHorizontal() {
        PilotLayout sub = new PilotLayout(m_ui, Direction.HORIZONTAL);
        m_children.add(sub);
        return sub;
    }

    // ── Padding ──

    @NonNull
    public PilotLayout addPadding(double weight) {
        m_children.add(new PaddingElement(weight));
        return this;
    }

    // ── Widgets ──

    @NonNull
    public PilotButton addButton(@NonNull String id, @NonNull String label) {
        PilotButton w = new PilotButton(m_ui, id, label);
        m_children.add(w);
        return w;
    }

    @NonNull
    public PilotLabel addLabel(@NonNull String id, @NonNull String text) {
        PilotLabel w = new PilotLabel(m_ui, id, text);
        m_children.add(w);
        return w;
    }

    @NonNull
    public PilotStat addStat(@NonNull String id, @NonNull String label) {
        PilotStat w = new PilotStat(m_ui, id, label);
        m_children.add(w);
        return w;
    }

    @NonNull
    public PilotSwitch addSwitch(@NonNull String id, @NonNull String label) {
        PilotSwitch w = new PilotSwitch(m_ui, id, label);
        m_children.add(w);
        return w;
    }

    @NonNull
    public PilotInput addInput(@NonNull String id, @NonNull String label) {
        PilotInput w = new PilotInput(m_ui, id, label);
        m_children.add(w);
        return w;
    }

    @NonNull
    public PilotSelect addSelect(@NonNull String id, @NonNull String label) {
        PilotSelect w = new PilotSelect(m_ui, id, label);
        m_children.add(w);
        return w;
    }

    @NonNull
    public PilotTextarea addTextarea(@NonNull String id, @NonNull String label) {
        PilotTextarea w = new PilotTextarea(m_ui, id, label);
        m_children.add(w);
        return w;
    }

    @NonNull
    public PilotTable addTable(@NonNull String id, @NonNull String label) {
        PilotTable w = new PilotTable(m_ui, id, label);
        m_children.add(w);
        return w;
    }

    @NonNull
    public PilotLogs addLogs(@NonNull String id, @NonNull String label) {
        PilotLogs w = new PilotLogs(m_ui, id, label);
        m_children.add(w);
        return w;
    }

    // ── Serialization ──

    @NonNull
    JSONObject toJson() {
        JSONObject json = new JSONObject();
        try {
            json.put("type", "layout");
            json.put("direction", m_direction.getValue());

            JSONArray childrenArr = new JSONArray();
            for (Object child : m_children) {
                if (child instanceof PilotLayout) {
                    childrenArr.put(((PilotLayout) child).toJson());
                } else if (child instanceof PilotWidget) {
                    childrenArr.put(((PilotWidget) child).toJson());
                } else if (child instanceof PaddingElement) {
                    childrenArr.put(((PaddingElement) child).toJson());
                }
            }
            json.put("children", childrenArr);
        } catch (JSONException ignored) {
        }
        return json;
    }

    // ── Padding helper ──

    static final class PaddingElement {
        final double weight;

        PaddingElement(double weight) {
            this.weight = weight;
        }

        JSONObject toJson() {
            JSONObject json = new JSONObject();
            try {
                json.put("type", "padding");
                json.put("weight", weight);
            } catch (JSONException ignored) {
            }
            return json;
        }
    }
}
