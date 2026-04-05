# Logging & Events

Pilot streams structured logs from your app to the dashboard in real time. Logs are buffered locally and flushed in batches.

## Log configuration

```java
PilotLogConfigBuilder logConfig = new PilotLogConfigBuilder()
    .setEnabled(true)                // default: true
    .setLogLevel(PilotLogLevel.INFO) // minimum level to capture
    .setBatchSize(100)               // logs per flush (default: 100)
    .setBufferSize(1000)             // max buffered logs before oldest are dropped (default: 1000)
    .setAttributes(logAttrs);        // attributes attached to every log entry

PilotConfig config = new PilotConfig.Builder(url, token)
    .setLogConfig(logConfig)
    .build();
```

## Log levels

`DEBUG` · `INFO` · `WARNING` · `ERROR` · `CRITICAL` · `EXCEPTION`

## Basic logging

```java
Pilot.log(PilotLogLevel.INFO, "Player spawned");
Pilot.log(PilotLogLevel.ERROR, "Failed to load asset");
```

With category and thread:

```java
Pilot.log(PilotLogLevel.WARNING, "Texture missing", "rendering", "GLThread");
```

With metadata (arbitrary key-value data per entry):

```java
Map<String, Object> meta = Map.of(
    "asset", "player.png",
    "size", 1024
);
Pilot.log(PilotLogLevel.ERROR, "Load failed", meta);
Pilot.log(PilotLogLevel.ERROR, "Load failed", "assets", "IOThread", meta);
```

## Structured log entries

For full control, build a `PilotLogEntry`:

```java
PilotLogEntry entry = PilotLogEntry.error("Crash detected")
    .setCategory("crash")
    .setThread("main")
    .setMetadata(Map.of("stack", stackTrace))
    .setTimestampMs(System.currentTimeMillis());

Pilot.log(entry);
```

Static factory methods: `.debug()`, `.info()`, `.warning()`, `.error()`, `.critical()`.

## Log attributes

Attributes are key-value pairs attached to **every** log entry. They can be static or dynamic:

```java
PilotLogAttributeBuilder logAttrs = new PilotLogAttributeBuilder()
    .put("app_version", "2.1.0")                          // static
    .putProvider("screen_name", () -> getCurrentScreen())  // dynamic
    .putProvider("user_score", () -> game.getScore());     // re-evaluated each flush
```

Pass them to the log config:

```java
new PilotLogConfigBuilder()
    .setAttributes(logAttrs);
```

## Events

Events are high-level structured messages with an implicit `EVENT` level:

```java
Pilot.event("level_completed");
Pilot.event("level_completed", Map.of("level", 5, "time", 42.3));
Pilot.event("level_completed", "gameplay", Map.of("level", 5));
Pilot.event("level_completed", "gameplay", Map.of("level", 5), timestampMs);
```

## Revenue events

Track in-app purchase events:

```java
Pilot.revenue("purchase_completed");
Pilot.revenue("purchase_completed", Map.of("amount", 4.99, "currency", "USD"));
Pilot.revenue("purchase_completed", "monetization", Map.of("sku", "gems_500"));
```

## Screen tracking

Report screen changes:

```java
Pilot.changeScreen("activity", "MainMenuActivity");
Pilot.changeScreen("fragment", "SettingsFragment");
```

## In-app products

Report available and owned products:

```java
Pilot.setInAppProducts(List.of(
    Map.of("id", "gems_100", "price", "$0.99", "type", "consumable"),
    Map.of("id", "no_ads",   "price", "$2.99", "type", "non-consumable")
));

Pilot.setOwnedInAppProducts(List.of("no_ads"));

Pilot.purchaseInApp("txn_123", List.of("gems_100"), Map.of("store", "google"));
```

## Custom SDK logger

Redirect the SDK's own diagnostic messages to your logging system:

```java
new PilotConfig.Builder(url, token)
    .setLoggerListener((level, tag, message) -> {
        Log.println(level, tag, message);
    });
```

---

**See also:** [Android Integration](android-integration.md) · [Widgets](widgets.md) · [Metrics](metrics.md)
