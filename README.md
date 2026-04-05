# Pilot SDK for Android

[![JitPack](https://jitpack.io/v/irov/javapilot.svg)](https://jitpack.io/#irov/javapilot)

Lightweight Android SDK for connecting apps to **Pilot** — a real-time remote debug panel. Inspect state, push commands, stream logs and control your app from a web dashboard while it runs on device.

## Features

- **Tabs & Layout** — each service adds its own tab with vertical/horizontal layouts
- **Widgets** — buttons, switches, stats, labels, inputs, selects, tables, logs, textareas
- **Value providers** — widgets auto-update via callbacks, dirty-checked on poll cycle
- **Remote actions** — trigger commands from dashboard with per-widget callbacks
- **App screen live** — on-demand LiveKit video live with remote tap/long-press control and in-app touch overlay
- **Log streaming** — structured logs with levels, categories, threads, per-entry attributes and metadata
- **Metrics** — time-series performance data (FPS, memory, CPU, network, custom) with collectors and charts
- **Session attributes** — static and dynamic key-value pairs attached to the session, native types preserved (boolean, int, float, null)
- **Log attributes** — static and dynamic key-value pairs attached to every log entry
- **Circular log buffer** — configurable buffer size and batch size for log flushing
- **Session management** — approval polling, action-poll liveness, lifecycle callbacks
- **Custom logger** — redirect SDK logs into your own logging system
- **Zero dependencies on engine** — works with any Android project

## Documentation

| Guide | Description |
|-------|-------------|
| [Android Integration](docs/android-integration.md) | Setup, configuration, session management |
| [Widgets](docs/widgets.md) | Tabs, layouts, all widget types, remote actions |
| [Logging & Events](docs/logging.md) | Structured logs, events, revenue tracking, screen changes |
| [Metrics](docs/metrics.md) | Performance metrics, collectors, custom metric types |
| [Live Streaming](docs/live-streaming.md) | Screen streaming via LiveKit, remote touch |
| [Self-Hosting](docs/self-hosting.md) | Deploy backend, dashboard, and LiveKit |

## Quick start

```groovy
// settings.gradle
dependencyResolutionManagement {
    repositories {
        maven { url 'https://jitpack.io' }
    }
}

// build.gradle
dependencies {
    implementation 'com.github.irov:javapilot:1.0.25'
}
```

```java
// Initialize
PilotConfig config = new PilotConfig.Builder(
        "https://pilot.example.com",
        "plt_your_api_token"
    )
    .setDeviceName("Pixel 8")
    .build();

Pilot.initialize(config, app);

// Build UI
PilotUI ui = Pilot.getUI();
PilotTab tab = ui.addTab("game", "Game Controls");
PilotLayout root = tab.vertical();

root.addButton("btn-restart", "Restart")
    .variant("contained").color("error")
    .onClick(action -> restartGame());

root.addStat("stat-fps", "FPS")
    .unit("fps")
    .valueProvider(() -> game.getFps());

// Connect
Pilot.connect();
```

See [Android Integration](docs/android-integration.md) for the full setup guide.

If you want to use the dashboard Live tab, initialize Pilot with an Application or a Context that resolves to an Application via getApplicationContext(). Live capture stays off by default and starts only after Stream is enabled from the web dashboard.

To enable the media pipeline, the backend must be configured with LiveKit credentials via `PLT_LIVEKIT_URL`, `PLT_LIVEKIT_API_KEY`, and `PLT_LIVEKIT_API_SECRET`.

## Connection flow

- While the device is waiting for approval, the SDK polls approval status every 10 seconds by default.
- After approval, action polling becomes the live session heartbeat.
- Dynamic session attributes are piggybacked on action polling, so there is no separate active-session heartbeat loop.
- You can still override timings with `setPollIntervalMs(...)` and `setActionPollIntervalMs(...)` if needed.

## App live

- Live capture is limited to the current app Activity, not the whole device screen.
- Video is published into LiveKit only while the dashboard requests live; normal session traffic does not carry the media feed.
- The dashboard can request different max-dimension / FPS presets, with low quality used by default.
- While live mode is active, the dashboard can send tap and long-press actions and the SDK draws a top-level touch indicator inside the app.

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
    .setEnabled(true)
    .setLogLevel(PilotLogLevel.INFO);

PilotConfig config = new PilotConfig.Builder(url, token)
    .setLoggerListener((level, tag, message, throwable) -> {
        MyLogger.log(tag + ": " + message);
    })
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
