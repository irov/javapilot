# Android Integration

Step-by-step guide for adding Pilot SDK to your Android project.

## Requirements

- Android API 21+
- Java 8+ or Kotlin

## 1. Add JitPack repository

In your root `settings.gradle`:

```groovy
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

## 2. Add the dependency

In your module `build.gradle`:

```groovy
dependencies {
    implementation 'com.github.irov:javapilot:1.0.25'
}
```

## 3. Configure the SDK

Create a `PilotConfig` and initialize the SDK in your `Application.onCreate()`:

```java
PilotConfig config = new PilotConfig.Builder(
        "https://pilot.example.com",  // your Pilot dashboard URL
        "plt_your_api_token"          // project API token from dashboard
    )
    .setDeviceName("Pixel 8")
    .build();

Pilot.initialize(config, this); // pass Application context
```

### Configuration options

| Method | Default | Description |
|--------|---------|-------------|
| `setDeviceId(String)` | auto | Custom device identifier |
| `setDeviceName(String)` | — | Human-readable device name shown in dashboard |
| `setPollIntervalMs(long)` | 10000 | Status polling interval (ms) |
| `setActionPollIntervalMs(long)` | 2000 | Action polling interval (ms) |
| `setAutoConnect(boolean)` | true | Connect automatically after init |
| `setLoggerListener(PilotLoggerListener)` | — | Redirect SDK diagnostic logs |
| `setSessionListener(PilotSessionListener)` | — | Session lifecycle callbacks |
| `setActionListener(PilotActionListener)` | — | Remote action callbacks |
| `setSessionAttributes(PilotSessionAttributeBuilder)` | — | Key-value pairs attached to the session |
| `setLogConfig(PilotLogConfigBuilder)` | — | Logging configuration |
| `setMetricConfig(PilotMetricConfigBuilder)` | — | Metrics configuration |

## 4. Session attributes

Attach static and dynamic key-value pairs to identify the session. Native types are preserved:

```java
PilotSessionAttributeBuilder sessionAttrs = new PilotSessionAttributeBuilder()
    .put("is_debug", BuildConfig.DEBUG)          // boolean
    .put("install_version", 42)                  // int
    .put("build_type", "debug")                  // string
    .put("referrer", null)                       // null
    .putProvider("user_id", () -> getUserId());   // dynamic — re-evaluated on each poll

PilotConfig config = new PilotConfig.Builder(url, token)
    .setSessionAttributes(sessionAttrs)
    .build();
```

## 5. Connect

If `autoConnect` is true (default), the SDK connects after `initialize()`. Otherwise call:

```java
Pilot.connect();
```

The SDK will:
1. Send a connect request to the backend.
2. Wait for approval from a dashboard user.
3. Start the debug session once approved.

## 6. Session status

Check the current state:

```java
PilotSessionStatus status = Pilot.getStatus();
```

Possible values: `DISCONNECTED`, `CONNECTING`, `WAITING_APPROVAL`, `ACTIVE`, `AUTH_FAILED`, `REJECTED`, `CLOSED`, `ERROR`.

## 7. Session lifecycle listener

```java
Pilot.addSessionListener(new PilotSessionListener() {
    @Override public void onPilotSessionConnecting() { }
    @Override public void onPilotSessionWaitingApproval(String requestId) { }
    @Override public void onPilotSessionStarted(String sessionToken) { }
    @Override public void onPilotSessionClosed() { }
    @Override public void onPilotSessionRejected() { }
    @Override public void onPilotSessionAuthFailed() { }
    @Override public void onPilotSessionError(PilotException e) { }
});
```

## 8. Disconnect & shutdown

```java
Pilot.disconnect();  // end current session
Pilot.shutdown();    // release all resources
```

## ProGuard / R8

The SDK ships its own `consumer-rules.pro`. No additional ProGuard configuration is needed.

---

**Next:** [Widgets](widgets.md) · [Logging & Events](logging.md) · [Metrics](metrics.md) · [Live Streaming](live-streaming.md)
