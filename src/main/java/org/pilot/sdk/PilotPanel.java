package org.pilot.sdk;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Builder for the debug panel layout sent to the Pilot server.
 * Supports all widget types: button, label, stat, input, switch, select, textarea, table, logs.
 *
 * <pre>{@code
 * PilotPanel panel = new PilotPanel();
 *
 * panel.addSection("controls", "Controls")
 *     .addButton("btn-restart", "Restart", "contained", "error")
 *     .addButton("btn-sync", "Sync", "outlined", "primary")
 *     .addSwitch("switch-debug", "Debug Mode", false);
 *
 * panel.addSection("info", "Status")
 *     .addStat("stat-fps", "FPS", "60", "fps")
 *     .addLabel("label-status", "Connected", "success");
 *
 * Pilot.INSTANCE.submitPanel(panel);
 * }</pre>
 */
public final class PilotPanel {
    private int m_version = 1;
    private int m_revision = 1;
    private final List<Section> m_sections = new ArrayList<>();

    public PilotPanel setVersion(int version) {
        m_version = version;
        return this;
    }

    public PilotPanel setRevision(int revision) {
        m_revision = revision;
        return this;
    }

    public int getRevision() {
        return m_revision;
    }

    public PilotPanel incrementRevision() {
        m_revision++;
        return this;
    }

    /**
     * Add a new section to the panel.
     *
     * @param id    Unique section identifier
     * @param title Section display title
     * @return Section builder for adding widgets (chainable)
     */
    @NonNull
    public Section addSection(@NonNull String id, @NonNull String title) {
        Section section = new Section(this, id, title);
        m_sections.add(section);
        return section;
    }

    @NonNull
    public JSONObject toJson() {
        JSONObject layout = new JSONObject();
        try {
            layout.put("version", m_version);
            layout.put("revision", m_revision);

            JSONArray sectionsArray = new JSONArray();
            for (Section section : m_sections) {
                sectionsArray.put(section.toJson());
            }
            layout.put("sections", sectionsArray);
        } catch (JSONException ignored) {
        }

        return layout;
    }

    /**
     * A section within the panel, containing widgets.
     */
    public static final class Section {
        private final PilotPanel m_panel;
        private final String m_id;
        private final String m_title;
        private final List<JSONObject> m_widgets = new ArrayList<>();

        Section(@NonNull PilotPanel panel, @NonNull String id, @NonNull String title) {
            m_panel = panel;
            m_id = id;
            m_title = title;
        }

        @NonNull
        public String getId() {
            return m_id;
        }

        // region Widget Builders

        @NonNull
        public Section addButton(@NonNull String id, @NonNull String label, @NonNull String variant, @NonNull String color) {
            JSONObject w = new JSONObject();
            try {
                w.put("id", id);
                w.put("type", "button");
                w.put("label", label);
                w.put("variant", variant);
                w.put("color", color);
            } catch (JSONException ignored) {
            }
            m_widgets.add(w);
            return this;
        }

        @NonNull
        public Section addLabel(@NonNull String id, @NonNull String text, @NonNull String color) {
            JSONObject w = new JSONObject();
            try {
                w.put("id", id);
                w.put("type", "label");
                w.put("text", text);
                w.put("color", color);
            } catch (JSONException ignored) {
            }
            m_widgets.add(w);
            return this;
        }

        @NonNull
        public Section addStat(@NonNull String id, @NonNull String label, @NonNull String value, @NonNull String unit) {
            JSONObject w = new JSONObject();
            try {
                w.put("id", id);
                w.put("type", "stat");
                w.put("label", label);
                w.put("value", value);
                w.put("unit", unit);
            } catch (JSONException ignored) {
            }
            m_widgets.add(w);
            return this;
        }

        @NonNull
        public Section addInput(@NonNull String id, @NonNull String label, @NonNull String inputType, @NonNull String defaultValue) {
            JSONObject w = new JSONObject();
            try {
                w.put("id", id);
                w.put("type", "input");
                w.put("label", label);
                w.put("inputType", inputType);
                w.put("defaultValue", defaultValue);
            } catch (JSONException ignored) {
            }
            m_widgets.add(w);
            return this;
        }

        @NonNull
        public Section addSwitch(@NonNull String id, @NonNull String label, boolean defaultValue) {
            JSONObject w = new JSONObject();
            try {
                w.put("id", id);
                w.put("type", "switch");
                w.put("label", label);
                w.put("defaultValue", defaultValue);
            } catch (JSONException ignored) {
            }
            m_widgets.add(w);
            return this;
        }

        @NonNull
        public Section addSelect(@NonNull String id, @NonNull String label, @NonNull String[][] options, @NonNull String defaultValue) {
            JSONObject w = new JSONObject();
            try {
                w.put("id", id);
                w.put("type", "select");
                w.put("label", label);
                w.put("defaultValue", defaultValue);

                JSONArray optionsArray = new JSONArray();
                for (String[] option : options) {
                    JSONObject opt = new JSONObject();
                    opt.put("value", option[0]);
                    opt.put("label", option[1]);
                    optionsArray.put(opt);
                }
                w.put("options", optionsArray);
            } catch (JSONException ignored) {
            }
            m_widgets.add(w);
            return this;
        }

        @NonNull
        public Section addTextarea(@NonNull String id, @NonNull String label, int rows) {
            JSONObject w = new JSONObject();
            try {
                w.put("id", id);
                w.put("type", "textarea");
                w.put("label", label);
                w.put("rows", rows);
            } catch (JSONException ignored) {
            }
            m_widgets.add(w);
            return this;
        }

        @NonNull
        public Section addTable(@NonNull String id, @NonNull String label, @NonNull String[][] columns, @NonNull List<JSONObject> rows) {
            JSONObject w = new JSONObject();
            try {
                w.put("id", id);
                w.put("type", "table");
                w.put("label", label);

                JSONArray columnsArray = new JSONArray();
                for (String[] col : columns) {
                    JSONObject colObj = new JSONObject();
                    colObj.put("key", col[0]);
                    colObj.put("label", col[1]);
                    columnsArray.put(colObj);
                }
                w.put("columns", columnsArray);

                JSONArray rowsArray = new JSONArray();
                for (JSONObject row : rows) {
                    rowsArray.put(row);
                }
                w.put("rows", rowsArray);
            } catch (JSONException ignored) {
            }
            m_widgets.add(w);
            return this;
        }

        @NonNull
        public Section addLogs(@NonNull String id, @NonNull String label, int maxLines) {
            JSONObject w = new JSONObject();
            try {
                w.put("id", id);
                w.put("type", "logs");
                w.put("label", label);
                w.put("maxLines", maxLines);
            } catch (JSONException ignored) {
            }
            m_widgets.add(w);
            return this;
        }

        /** Add a fully custom widget JSON. */
        @NonNull
        public Section addWidget(@NonNull JSONObject widget) {
            m_widgets.add(widget);
            return this;
        }

        // endregion

        // region Widget Update Helpers

        /**
         * Update the "value" field of a stat widget in this section.
         */
        @NonNull
        public Section updateStatValue(@NonNull String widgetId, @NonNull String newValue) {
            for (JSONObject w : m_widgets) {
                if (widgetId.equals(w.optString("id")) && "stat".equals(w.optString("type"))) {
                    try {
                        w.put("value", newValue);
                    } catch (JSONException ignored) {
                    }
                    break;
                }
            }
            return this;
        }

        /**
         * Update the "text" field of a label widget in this section.
         */
        @NonNull
        public Section updateLabelText(@NonNull String widgetId, @NonNull String newText) {
            for (JSONObject w : m_widgets) {
                if (widgetId.equals(w.optString("id")) && "label".equals(w.optString("type"))) {
                    try {
                        w.put("text", newText);
                    } catch (JSONException ignored) {
                    }
                    break;
                }
            }
            return this;
        }

        /**
         * Update the "text" field of a label widget with a new color.
         */
        @NonNull
        public Section updateLabelText(@NonNull String widgetId, @NonNull String newText, @NonNull String newColor) {
            for (JSONObject w : m_widgets) {
                if (widgetId.equals(w.optString("id")) && "label".equals(w.optString("type"))) {
                    try {
                        w.put("text", newText);
                        w.put("color", newColor);
                    } catch (JSONException ignored) {
                    }
                    break;
                }
            }
            return this;
        }

        // endregion

        /** Return to the parent panel builder. */
        @NonNull
        public PilotPanel done() {
            return m_panel;
        }

        @NonNull
        JSONObject toJson() {
            JSONObject json = new JSONObject();
            try {
                json.put("id", m_id);
                json.put("title", m_title);

                JSONArray widgetsArray = new JSONArray();
                for (JSONObject widget : m_widgets) {
                    widgetsArray.put(widget);
                }
                json.put("widgets", widgetsArray);
            } catch (JSONException ignored) {
            }

            return json;
        }
    }
}
