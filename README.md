# Pilot SDK for Android

[![JitPack](https://jitpack.io/v/irov/javapilot.svg)](https://jitpack.io/#irov/javapilot)

Lightweight Android SDK for connecting apps to **Pilot** — a real-time remote debug panel. Inspect state, push commands, stream logs and control your app from a web dashboard while it runs on device.

## Features

- **Tabs & Layout** — each service adds its own tab with vertical/horizontal layouts
- **Widgets** — buttons, switches, stats, labels, inputs, selects, tables, logs, textareas
- **Value providers** — widgets auto-update via callbacks, dirty-checked on poll cycle
- **Remote actions** — trigger commands from dashboard with per-widget callbacks
- **Log streaming** — structured logs with levels, categories, threads, per-entry attributes and metadata
- **Session attributes** — static and dynamic key-value pairs attached to the session, native types preserved (boolean, int, float, null)
- **Log attributes** — static and dynamic key-value pairs attached to every log entry
- **Circular log buffer** — configurable buffer size and batch size for log flushing
- **Session management** — approval polling, action-poll liveness, lifecycle callbacks
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
    implementation 'com.github.irov:javapilot:1.0.17'
}
```

## Quick start

```java
// Session attributes — native types preserved (bool, int, float, null)
PilotSessionAttributeBuilder sessionAttrs = new PilotSessionAttributeBuilder()
    .put("is_debug", BuildConfig.DEBUG)          // boolean
    .put("install_version", 42)                  // int
    .put("build_type", "debug")                  // string
    .put("referrer", null)                       // null
    .putProvider("user_id", () -> getUserId());   // dynamic provider

// Log attributes — attached to every log entry
PilotLogAttributeBuilder logAttrs = new PilotLogAttributeBuilder()
    .putProvider("user_score", () -> game.getScore())  // float, changes dynamically
    .putProvider("screen_name", () -> getCurrentScreen());

// Initialize
PilotConfig config = new PilotConfig.Builder(
        "https://pilot.example.com",
        "plt_your_api_token"
    )
    .setDeviceName("Pixel 8")
    .setSessionAttributes(sessionAttrs)
    .setLogAttributes(logAttrs)
    .setLogBatchSize(100)    // logs per flush cycle (default 100)
    .setLogBufferSize(1000)  // max buffered logs (default 1000)
    .build();

Pilot.initialize(config);

// Build UI — each service adds its own tab
PilotUI ui = Pilot.getUI();
PilotTab tab = ui.addTab("game", "Game Controls");
PilotLayout root = tab.vertical();

// Buttons with callbacks
root.addButton("btn-restart", "Restart")
    .variant("contained").color("error")
    .onClick(action -> restartGame());

// Stats with value providers — auto-updated, dirty-checked
root.addStat("stat-fps", "FPS")
    .unit("fps")
    .valueProvider(() -> game.getFps());

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

## Connection flow

- While the device is waiting for approval, the SDK polls approval status every 10 seconds by default.
- After approval, action polling becomes the live session heartbeat.
- Dynamic session attributes are piggybacked on action polling, so there is no separate active-session heartbeat loop.
- You can still override timings with `setPollIntervalMs(...)` and `setActionPollIntervalMs(...)` if needed.

## Logging

```java
// Simple log
Pilot.log(PilotLogLevel.INFO, "Game started");

// Log with category and thread
Pilot.log(PilotLogLevel.WARNING, "Low memory", "system", "main");

// Log with metadata (native Map instead of JSONObject)
Map<String, Object> meta = Map.of(
    "heap_mb", Runtime.getRuntime().totalMemory() / 1024 / 1024,
    "is_low", true
);
Pilot.log(PilotLogLevel.ERROR, "OOM crash", meta);

// Full log call
Pilot.log(PilotLogLevel.ERROR, "Crash detected", "crashes", "worker-3", meta);
```

## Custom logger

```java
PilotLogConfigBuilder logConfig = new PilotLogConfigBuilder()
    .setLoggerListener((level, tag, message, throwable) -> {
        MyLogger.log(tag + ": " + message);
    });

PilotConfig config = new PilotConfig.Builder(url, token)
    .setLogConfig(logConfig)
    .build();
```

Internal SDK diagnostic logs are delivered on the action poll cycle. There is no separate log flush interval.

Metrics are sampled locally at `sampleIntervalMs` and flushed on the same poll cycle, preserving their client timestamps.

The action poll request also acts as the session heartbeat. There is no separate heartbeat request.

## Requirements

- Android API 21+
- Java 8+

## License

MIT
