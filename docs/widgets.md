# Widgets

Pilot UI is built with tabs, layouts, and widgets. Each service in your app can add its own tab on the dashboard.

## Tabs

```java
PilotUI ui = Pilot.getUI();

PilotTab tab = ui.addTab("game", "Game Controls");

// Retrieve or remove tabs later
PilotTab existing = ui.getTab("game");
ui.removeTab("game");
```

## Layouts

Every tab starts with a layout direction:

```java
PilotLayout root = tab.vertical();   // top-to-bottom
PilotLayout root = tab.horizontal(); // left-to-right
```

Nest layouts for complex arrangements:

```java
PilotLayout row = root.addHorizontal();
row.addButton("btn-a", "Left");
row.addPadding(1.0);                 // flexible spacer
row.addButton("btn-b", "Right");
```

### Collapsible sections

```java
PilotLayout section = root.addCollapsible("Advanced");
section.addSwitch("sw-debug", "Verbose logging");
```

## Widget types

### Button

```java
root.addButton("btn-restart", "Restart")
    .variant("contained")   // "contained" | "outlined" | "text"
    .color("error")          // MUI color: "primary" | "error" | "warning" | "info" | "success"
    .disabled(false)
    .onClick(action -> restartGame());
```

### Label

Static or dynamic text:

```java
root.addLabel("label-status", "Idle")
    .color("info")
    .textProvider(() -> game.getStatus());  // auto-updated
```

### Stat

Numeric value with unit:

```java
root.addStat("stat-fps", "FPS")
    .unit("fps")
    .value(60)                              // static
    .valueProvider(() -> game.getFps());    // or dynamic
```

### Switch

Boolean toggle:

```java
root.addSwitch("sw-godmode", "God mode")
    .defaultValue(false)
    .onChange(action -> {
        game.setGodMode(action.getValue());
    });
```

### Input

Single-line text input:

```java
root.addInput("input-cmd", "Command")
    .inputType("text")       // "text" | "number" | "password"
    .placeholder("type a command…")
    .defaultValue("")
    .onSubmit(action -> {
        executeCommand(action.getValue());
    });
```

### Select

Dropdown with options:

```java
root.addSelect("sel-level", "Level")
    .options(new String[][]{
        {"easy",   "Easy"},
        {"normal", "Normal"},
        {"hard",   "Hard"}
    })
    .defaultValue("normal")
    .onChange(action -> {
        game.setDifficulty(action.getValue());
    });
```

### Textarea

Multi-line text area:

```java
root.addTextarea("ta-notes", "Notes")
    .rows(5)
    .defaultValue("")
    .onSubmit(action -> {
        saveNotes(action.getValue());
    });
```

### Table

Read-only data table:

```java
root.addTable("tbl-inventory", "Inventory")
    .columns(new String[][]{
        {"name", "Item"},
        {"qty",  "Quantity"}
    })
    .rows(List.of(
        new JSONObject().put("name", "Sword").put("qty", 1),
        new JSONObject().put("name", "Potion").put("qty", 5)
    ));
```

### Logs

Dedicated log viewer widget:

```java
root.addLogs("log-viewer", "App Logs")
    .maxLines(500);
```

## Value providers

Widgets that accept a `valueProvider()` or `textProvider()` are dirty-checked on each poll cycle. The dashboard updates only when the value changes.

```java
root.addStat("stat-score", "Score")
    .valueProvider(() -> game.getScore());
```

## Remote actions

All interactive widgets (`Button`, `Switch`, `Input`, `Select`, `Textarea`) fire callbacks when the user interacts from the dashboard. The callback receives a `PilotAction`:

```java
action.getId();          // unique action UUID
action.getWidgetId();    // widget that triggered it
action.getActionType();  // CLICK, CHANGE, TOGGLE
action.getPayload();     // JSONObject with action data
```

### Action types

| Type | Widgets | Payload |
|------|---------|---------|
| `CLICK` | Button | — |
| `CHANGE` | Select | `{"value": "..."}` |
| `TOGGLE` | Switch | `{"value": true/false}` |
| `CLICK` | Input, Textarea (submit) | `{"value": "..."}` |

### Global action listener

Receive all actions in one place:

```java
Pilot.addActionListener(action -> {
    Log.d("Pilot", "Action on " + action.getWidgetId());
});
```

Acknowledge actions with an optional result payload:

```java
Pilot.acknowledgeAction(action.getId(), new JSONObject().put("ok", true));
```

---

**See also:** [Android Integration](android-integration.md) · [Logging & Events](logging.md) · [Metrics](metrics.md)
