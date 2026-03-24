# Pilot SDK for Android

[![JitPack](https://jitpack.io/v/irov/javapilot.svg)](https://jitpack.io/#irov/javapilot)

Lightweight Android SDK for connecting apps to **Pilot** — a real-time remote debug panel. Inspect state, push commands, stream logs and control your app from a web dashboard while it runs on device.

## Features

- **Live panel** — build a custom UI (buttons, switches, stats, inputs) visible in the web dashboard
- **Remote actions** — trigger commands from dashboard, handle them in app
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

// Build panel
PilotPanel panel = new PilotPanel();
panel.addSection("stats", "Stats")
    .addStat("fps", "FPS", "60", "fps")
    .addButton("restart", "Restart", "contained", "error");

Pilot.submitPanel(panel);

// Handle actions
Pilot.addActionListener(action -> {
    if ("restart".equals(action.getWidgetId())) {
        restartGame();
        Pilot.acknowledgeAction(action.getId(), null);
    }
});

// Connect
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
