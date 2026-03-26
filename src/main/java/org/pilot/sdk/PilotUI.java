package org.pilot.sdk;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Central UI data source for the Pilot SDK.
 *
 * <p>Each service/module adds its own tab. Widgets inside tabs automatically
 * route action callbacks when a dashboard user interacts with them.</p>
 *
 * <pre>{@code
 * PilotUI ui = Pilot.getUI();
 *
 * PilotTab tab = ui.addTab("Game Controls");
 * PilotLayout root = tab.vertical();
 *
 * root.addButton("Restart")
 *     .variant("contained").color("error")
 *     .onClick(action -> restartGame());
 *
 * root.addStat("FPS").value("60").unit("fps");
 *
 * PilotLayout row = root.addHorizontal();
 * row.addButton("Action A").onClick(action -> doA());
 * row.addPadding(1.0);
 * row.addButton("Action B").onClick(action -> doB());
 * }</pre>
 */
public final class PilotUI {
    private final List<PilotTab> m_tabs = new ArrayList<>();
    private final Map<Integer, PilotWidgetCallback> m_callbacks = new ConcurrentHashMap<>();
    private final Set<PilotWidget<?>> m_providers = ConcurrentHashMap.newKeySet();
    private final AtomicInteger m_idCounter = new AtomicInteger(0);
    private int m_version = 2;
    private final AtomicInteger m_revision = new AtomicInteger(1);
    private int m_sentRevision = 0;

    PilotUI() {
    }

    // ── ID generation ──

    int nextId() {
        return m_idCounter.incrementAndGet();
    }

    // ── Tab management ──

    /**
     * Add a new tab (or replace an existing one with the same title).
     *
     * @param title Tab display title (also used as default id)
     * @return The new tab for building its layout
     */
    @NonNull
    public PilotTab addTab(@NonNull String title) {
        m_tabs.removeIf(t -> t.getTitle().equals(title));
        PilotTab tab = new PilotTab(this, title);
        m_tabs.add(tab);
        m_revision.incrementAndGet();
        return tab;
    }

    /**
     * Get an existing tab by public id.
     */
    @Nullable
    public PilotTab getTab(@NonNull String id) {
        for (PilotTab tab : m_tabs) {
            if (tab.getId().equals(id)) {
                return tab;
            }
        }
        return null;
    }

    /**
     * Remove a tab by public id.
     */
    public void removeTab(@NonNull String id) {
        m_tabs.removeIf(t -> t.getId().equals(id));
        m_revision.incrementAndGet();
    }

    // ── Widget callbacks ──

    void registerCallback(int widgetId, @Nullable PilotWidgetCallback callback) {
        if (callback != null) {
            m_callbacks.put(widgetId, callback);
        } else {
            m_callbacks.remove(widgetId);
        }
    }

    /**
     * Dispatch an action to the registered widget callback.
     *
     * @return true if a callback was found and invoked
     */
    boolean dispatchAction(@NonNull PilotAction action) {
        PilotWidgetCallback cb = m_callbacks.get(action.getWidgetId());
        if (cb != null) {
            cb.onPilotWidgetAction(action);
            return true;
        }
        return false;
    }

    // ── Value providers ──

    void registerProvider(@NonNull PilotWidget<?> widget) {
        m_providers.add(widget);
    }

    void unregisterProvider(@NonNull PilotWidget<?> widget) {
        m_providers.remove(widget);
    }

    /**
     * Poll all value providers. If any value changed, increments revision.
     * Called on the SDK poll thread.
     */
    void pollValues() {
        for (PilotWidget<?> w : m_providers) {
            if (w.pollProvider()) {
                m_revision.incrementAndGet();
            }
        }
    }

    // ── Revision tracking ──

    /**
     * Increment UI revision. Called automatically by tabs/widgets/layouts
     * when structure or values change.
     */
    void incrementRevision() {
        m_revision.incrementAndGet();
    }

    /**
     * Whether there are unsent changes.
     */
    boolean hasUnsent() {
        return m_revision.get() != m_sentRevision;
    }

    /**
     * Mark the current revision as sent.
     */
    void markSent() {
        m_sentRevision = m_revision.get();
    }

    boolean hasTabs() {
        return !m_tabs.isEmpty();
    }

    public int getRevision() {
        return m_revision.get();
    }

    // ── Serialization ──

    @NonNull
    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        try {
            json.put("version", m_version);
            json.put("revision", m_revision.get());

            JSONArray tabsArr = new JSONArray();
            for (PilotTab tab : m_tabs) {
                tabsArr.put(tab.toJson());
            }
            json.put("tabs", tabsArr);
        } catch (JSONException ignored) {
        }
        return json;
    }
}
