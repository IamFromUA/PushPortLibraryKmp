# PushPort Kotlin Multiplatform SDK

Shared Kotlin API over the native PushPort Android, Apple and Web SDKs. **Version 0.0.1; Maven Central publication pending.** This package delegates token registration, persistence, permissions and delivery to the platform SDK. It creates no extra installation, HTTP client or competing notification handler.

| Target | Adapter | Requirements |
| --- | --- | --- |
| Android | Native `dev.pushport:android-sdk:0.0.1` | Android API 23+, foreground Activity for permission prompts |
| iOS device / Apple Silicon and Intel simulator | Objective-C bridge implemented by PushPort Swift package | iOS 15+, macOS/Xcode to build, PushPort SPM product linked in the host |
| Kotlin/JS browser | Native `@pushport/web-sdk` module | Secure browser context, same-origin service worker |
| JVM | Common facade / custom driver and portable tests | No native JVM notification delivery |

Kotlin/Wasm and desktop notification providers are not implemented.

## Dependency

After the Maven Central release is available, add `mavenCentral()` and `google()` to your repositories and the dependency to the shared module:

```kotlin
commonMain.dependencies {
    implementation("dev.pushport:kmp-sdk:0.0.1")
}
```

Android's native AAR is a normal transitive Gradle dependency. iOS hosts additionally link the **PushPort** Swift Package Manager product. The current stable interop path deliberately keeps Swift package linking in Xcode; there is no dependency on experimental SwiftPM-import Gradle tooling. Web hosts install/import the Web SDK once and pass its module to the adapter.

## Common API

```kotlin
import dev.pushport.kmp.PushPort
import dev.pushport.kmp.PushPortConfiguration

// Receive the platform-created client through your application DI/constructor.
suspend fun configurePush(client: PushPort) {
    client.initialize(PushPortConfiguration(appId = "YOUR_PUSHPORT_APP_ID"))
    client.setLocale("uk-UA")
}

// From your permission/subscription UI:
// client.requestPermission()
// client.setSubscribed(false)
// client.setLocale(null)
// client.sync()
// val status = client.status()
```

Call platform-facing operations from the UI/main thread. On Android, requesting permission starts the native permission flow; it does not await the user's decision. Read status after the permission result / on resume. On Apple/Web, the native prompt completes asynchronously. A successful subscription mutation means the native SDK accepted the operation; consult lastSyncedAt/syncError for server synchronization.

### Android

```kotlin
val client = createAndroidPushPort(applicationContext) { currentForegroundActivity }
```

The Activity provider must return a live Activity and must not retain a destroyed one. The SDK retains only applicationContext. Keep the client in your application scope and supply the current Activity from the host lifecycle. Initialization invokes the existing native Android SDK with the same App ID/server.

### Apple

```kotlin
// iosMain, main thread
val client = createApplePushPort()
client.initialize(PushPortConfiguration(
    appId = "YOUR_PUSHPORT_APP_ID",
    appleEnvironment = AppleEnvironment.SANDBOX,
))
```

In Xcode link the PushPort **SPM product** to the iOS application, enable Push Notifications, and forward APNs callbacks as described in the Apple SDK README. Explicitly instantiate `PushPortKMPBridge()` once in the Swift host during startup to ensure its Objective-C class is linked before the Kotlin factory is called (see [example](examples/AppleHost.swift)). The common and native initialization must use the same configuration. For images, the extension depends on the Apple extension product directly.

The checked-in Cinterop header declares stable selectors, not a duplicate native implementation. The KMP KLIB alone cannot contain a Swift package added separately through SPM. Test the final linked application, not only compilation of Kotlin metadata.

### Web / Kotlin JS

```kotlin
@JsModule("@pushport/web-sdk")
external object NativeWebSdk

val client = createWebPushPort(NativeWebSdk, serviceWorkerUrl = "/pushport-sw.js")
```

Add `implementation(npm("@pushport/web-sdk", "0.0.1"))` to the host's `jsMain.dependencies` after npm publication and copy the native SDK worker into the site's public directory. SSR import is safe, but initialize the client only in a supported browser. Request permission from an actual user click.

## Development and release

Use the checked-in wrapper, JDK 17, Node.js 24 and Android SDK 36. Portable verification:

```text
./gradlew jvmTest jsNodeTest
```

Until Android is public in Maven Central, native adapter verification can use an **explicit** local Maven repository:

```text
./gradlew compileAndroidMain testAndroidHostTest -Ppushport.androidRepository=/absolute/path/to/android-sdk/build/maven-repository
```

There is no hardcoded dependency on sibling directories. For iOS run `compileKotlinIosArm64 compileKotlinIosSimulatorArm64 compileKotlinIosX64` on macOS, then run `bash scripts/verify-apple-consumer.sh "$PWD/build/maven-repository"` after local publication to link an independent KMP application with the public Swift package. [Architecture/API](docs/architecture.md), [publishing](docs/publishing.md), [security](SECURITY.md).

The Apple SDK is published/tested first; publish KMP after compatible Android/Apple versions and Web npm package exist. Package availability and provider delivery are separate. Check the platform status in the PushPort console before testing real notifications.

Apache-2.0 В· Oleh Yurkov В· support@pushport.dev

Source: [PushPortLibraryKmp](https://github.com/IamFromUA/PushPortLibraryKmp) · [Integration documentation](https://pushport.dev/en/docs/)
