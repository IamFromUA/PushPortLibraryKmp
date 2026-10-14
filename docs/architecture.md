# Architecture and API

| Surface | Meaning |
| --- | --- |
| `initialize(configuration)` | Bind once to an App ID and server; repeated same configuration is accepted |
| `status()` | Local snapshot without raw tokens, bearer credentials or Web subscription keys |
| `sync()` | Ask the native SDK to reconcile metadata, token and subscription |
| `setSubscribed(boolean)` | Native persistent application preference; permission remains separate |
| `setLocale(tag or null)` | Validated locale override or automatic platform locale |
| `requestPermission()` | Native prompt from the host UI; Android requires the foreground Activity |

`PushPort` provides common preconditions and one immutable binding. `PushPortDriver` is a small replaceable platform boundary. Factory functions select the actual Android, Apple or Web adapter. Tests substitute the driver and verify behavior, rather than making real network/provider requests.

On Android the adapter directly invokes the released AAR. On Apple Cinterop calls the stable `PPPushPortBridge` Objective-C class in the Swift SDK. The header must match the exported selectors and nullability; keep the bridge contract compatible. On Web the adapter invokes the actual TypeScript SDK, normalizes its permission state and observes its promise results. No adapter independently generates an installation ID or contacts the backend.

Keep provider-specific features in native APIs until the common abstraction can express them honestly. Current common API does not promise identical permission-prompt completion timing or native notification click callbacks. Apple hosts use the Swift callback; Android hosts use the native SDK/intent path; Web routes through its service worker.

Version 0.0.2 compatibility: Android SDK 0.0.2, Apple bridge 0.0.2, Web SDK 0.0.2. Native binaries/platform bindings must be tested together for each KMP release. Source compatibility of Kotlin alone does not prove Swift link compatibility.
