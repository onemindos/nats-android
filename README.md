# nats-android

OneMind OS NATS leaf node SDK for Android — persistent background service, AIDL IPC, Kotlin Flow API.

Connects Android devices to the OneMind NATS mesh at `wss://ws.onemindos.dev` as first-class participants: publish, subscribe, and receive intelligence feeds from any OneMind agent or TAK bridge.

---

## Modules

| Module | Description |
|--------|-------------|
| `natssdk` | Android library — the SDK itself |
| `app` | Demo app showing connect / publish / subscribe |

---

## Quick start

### 1. Add the dependency

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        maven("https://maven.pkg.github.com/onemindos/nats-android") {
            credentials {
                username = providers.gradleProperty("gpr.user").orNull
                password = providers.gradleProperty("gpr.token").orNull
            }
        }
    }
}
```

```kotlin
// build.gradle.kts
dependencies {
    implementation("dev.onemindos:natssdk:1.0.0")
}
```

Or include the module directly:

```kotlin
include(":natssdk")
project(":natssdk").projectDir = file("../nats-android/natssdk")
```

### 2. Connect

```kotlin
val client = NatsLeafClient(
    context = this,
    config = NatsLeafConfig(
        serverUrl = "wss://ws.onemindos.dev",
        jwtCredentials = credsFileContent,   // .creds file from tak-nats-leaf secret
    )
)

client.connect {
    // called once the service is bound and ready
}
```

### 3. Publish

```kotlin
client.publish("onemind.cot", cotBytes)
client.publish("onemind.ping", "hello from android")
```

### 4. Subscribe (Kotlin Flow)

```kotlin
lifecycleScope.launch {
    client.messages("onemind.>").collect { (subject, payload) ->
        Log.d("NATS", "$subject → ${payload.decodeToString()}")
    }
}
```

### 5. Disconnect

```kotlin
client.disconnect()
```

---

## Architecture

```
┌─────────────────────────────────┐
│         Your App / ATAK Plugin  │
│                                 │
│   NatsLeafClient                │
│   ├── connect()                 │
│   ├── publish(subject, payload) │
│   └── messages(subject): Flow   │
└────────────┬────────────────────┘
             │ bindService (AIDL)
             ▼
┌─────────────────────────────────┐
│       NatsBridgeService         │  ← Foreground service, START_STICKY
│       (background, persistent)  │
│                                 │
│   NatsLeafConnection            │
│   └── nats.java client          │
└────────────┬────────────────────┘
             │ wss://
             ▼
┌─────────────────────────────────┐
│   ws.onemindos.dev              │  ← NATS 2.x WebSocket
│   OneMind NATS cluster          │
└─────────────────────────────────┘
```

**Key design decisions:**

- **Single connection per device** — all apps bind to one `NatsBridgeService`, one NATS connection regardless of how many clients are bound
- **AIDL IPC** — typed inter-process communication so ATAK plugins (separate process) can pub/sub without owning the connection
- **`START_STICKY`** — Android restarts the service automatically if killed; reconnect is handled by `nats.java` internally
- **Foreground service** — required on Android 8+ for persistent background work; shows a low-priority notification

---

## Configuration

```kotlin
NatsLeafConfig(
    serverUrl = "wss://ws.onemindos.dev",  // WebSocket URL
    nkeySeed = "SUABC...",                 // NKey seed (starts with SU), OR
    jwtCredentials = "-----BEGIN NATS...", // full .creds file content
    caCertPem = null,                      // null = system trust store
    reconnectWaitMs = 2_000,               // 2s between reconnect attempts
    maxReconnects = -1,                    // -1 = infinite retries
    notificationChannelId = "nats_bridge",
    notificationTitle = "OneMind NATS",
)
```

### Credentials

Get credentials from the `tak-nats-leaf` Kubernetes secret:

```bash
kubectl get secret -n takserver tak-nats-leaf -o jsonpath='{.data.leaf\.conf}' | base64 -d
```

Pass the full `.creds` file content as `jwtCredentials`.

---

## ATAK Plugin integration

```kotlin
class MyAtakPlugin : AbstractPlugin() {

    private lateinit var nats: NatsLeafClient

    override fun onStart() {
        nats = NatsLeafClient(pluginContext, NatsLeafConfig(
            serverUrl = "wss://ws.onemindos.dev",
            jwtCredentials = loadCreds(),
        ))
        nats.connect()

        pluginScope.launch {
            nats.messages("onemind.cot.>").collect { (_, payload) ->
                // inject CoT into ATAK map
                CotMapComponent.getInstance().dispatchEvent(parseCot(payload))
            }
        }
    }

    override fun onStop() = nats.disconnect()
}
```

---

## Requirements

- Android 8.0+ (API 26)
- `INTERNET` permission
- `FOREGROUND_SERVICE` + `POST_NOTIFICATIONS` permissions (SDK handles both)

---

## License

Apache 2.0 — OneMind OS
