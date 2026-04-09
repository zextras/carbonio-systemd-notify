# carbonio-systemd-notify

Native `sd_notify(3)` for Java via the Foreign Function & Memory (FFM) API — zero dependencies, no-op outside systemd.

## What it does

Calls `sd_notify(3)` directly from `libsystemd.so.0` inside the JVM process, so systemd gets `READY=1` from the **main PID**. This enables `NotifyAccess=main` (tightest security) and eliminates shell polling wrappers.

When `NOTIFY_SOCKET` is not set (dev, test, non-systemd environments), **nothing happens** — no native library is loaded, no FFM code executes.

## Usage

```java
import com.zextras.carbonio.systemd.SystemdNotify;

// After your server is ready (port bound, dependencies up):
SystemdNotify.ready("myservice ready on port 8080");

// Or without a status message:
SystemdNotify.ready(null);
```

### Quarkus

```java
@ApplicationScoped
public class SystemdReadinessNotifier {
    void onStart(@Observes StartupEvent ev) {
        SystemdNotify.ready("catalog ready");
    }
}
```

### Jetty / standalone

```java
server.start();
SystemdNotify.ready("mailbox ready");
```

### systemd unit

```ini
[Service]
Type=notify
NotifyAccess=main
ExecStart=/path/to/java \
  --enable-preview \
  --enable-native-access=ALL-UNNAMED \
  -jar myservice.jar
```

## Maven dependency

```xml
<dependency>
  <groupId>com.zextras.carbonio</groupId>
  <artifactId>carbonio-systemd-notify</artifactId>
  <version>1.0.0-SNAPSHOT</version>
</dependency>
```

## Requirements

| Requirement | Details |
|---|---|
| Java | 21+ (`--enable-preview`) or 22+ (FFM stable) |
| JVM flags | `--enable-preview --enable-native-access=ALL-UNNAMED` |
| Runtime | `libsystemd.so.0` (present on any systemd-based Linux) |
| Compiler | `--enable-preview` in `maven-compiler-plugin` |
| Tests | `--enable-preview --enable-native-access=ALL-UNNAMED` in surefire/failsafe `argLine` |

## Design

- **Single class** (`SystemdNotify`), no transitive dependencies
- **JUL logging** (`java.util.logging`) — works in every framework without conflicts
- **Static initializer** short-circuits when `NOTIFY_SOCKET` is absent
- **Catches `Throwable`** — degrades gracefully if `--enable-native-access` is missing
- The `sd_notify(0, "READY=1\nSTATUS=...")` call is made from the main JVM process, so systemd tracks the correct PID

## License

AGPL-3.0-only — see [COPYING](COPYING).
