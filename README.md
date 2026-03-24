# Pilot SDK for Android

[![JitPack](https://jitpack.io/v/irov/javapilot.svg)](https://jitpack.io/#irov/javapilot)

Lightweight Android SDK for connecting apps to **Pilot** — a real-time remote debug panel. Inspect state, push commands, stream logs and control your app from a web dashboard while it runs on device.

## Features

- **Tabs & Layout** — each service adds its own tab with vertical/horizontal layouts
- **Widgets** — buttons, switches, stats, labels, inputs, selects, tables, logs
- **Value providers** — widgets auto-update via callbacks, dirty-checked on poll cycle
- **Remote actions** — trigger commands from dashboard with per-widget callbacks
- **Log streaming** — send structured logs with levels and metadata
- **Session management** — auto heartbeat, reconnection, lifecycle callbacks
- **Custom logger** — redirect SDK logs into your own logging system
- **Zero dependencies on engine** — works with any Android project

## Setup

Add JitPack repository and dependency:

```groovy
// settings.gradle
dependencyResolutionManagement {
    repositories {
        maven { url 'https://jitpack.io' }
    }
}

// build.gradle
dependencies {
    implementation 'com.github.irov:javapilot:<version>'
}
```

## Quick start

```java
// Initialize
PilotConfig config = new PilotConfig.Builder(
        "https://pilot.example.com",
        "plt_your_api_token"
    )
    .setDeviceName("Pixel 8")
    .build();

Pilot.initialize(config);

// Build UI — each service adds its own tab
PilotUI ui = Pilot.getUI();
PilotTab tab = ui.addTab("game", "Game Controls");
PilotLayout root = tab.vertical();

// Buttons with callbacks
root.addButton("btn-restart", "Restart")
    .variant("contained").color("error")
    .onClick(action -> {
        restartGame();
        Pilot.acknowledgeAction(action.getId(), null);
    });

// Stats with value providers — auto-updated, dirty-checked
root.addStat("stat-fps", "FPS")
    .unit("fps")
    .valueProvider(() -> String.valueOf(game.getFps()));

// Horizontal row with padding
PilotLayout row = root.addHorizontal();
row.addButton("btn-a", "Action A").onClick(action -> doA());
row.addPadding(1.0);
row.addButton("btn-b", "Action B").onClick(action -> doB());

// Labels with text providers
root.addLabel("label-status", "Idle")
    .color("info")
    .textProvider(() -> game.getStatus());

// Connect — UI is sent automatically on session start
// and re-sent when widgets change
Pilot.connect();
```

## Custom logger

```java
PilotConfig config = new PilotConfig.Builder(url, token)
    .setLogger((level, tag, message, throwable) -> {
        MyLogger.log(tag + ": " + message);
    })
    .build();
```

## Requirements

- Android API 21+
- Java 8+

## License

MIT
