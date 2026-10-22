# Version 0.0.2 verification — 2026-10-14

Twelve portable/Android tests, all Android/JVM/JS/iOS device and simulator Maven targets, and final independent Kotlin framework plus Swift app/extension linking passed on macOS with Apple SDK 0.0.2. KMP 0.0.2 is published on Maven Central and uses Android SDK 0.0.5. The Web host installs its native 0.0.2 GitHub npm archive. These checks do not send real notifications.

Evidence: [successful release verification](https://github.com/IamFromUA/PushPortLibraryKmp/actions/runs/38542181151). The run commit matches the immutable public 0.0.2 tag.

## Historical 0.0.1 verification

The following results describe the initial September release, not the current test counts or publication status.

### Release verification — 2026-09-13

- JVM, Android host and Node.js common-behavior checks passed (3 tests per target).
- All Maven targets were built on macOS: Android, JVM, JS, iOS arm64, iOS simulator arm64 and x64, plus root metadata.
- An independent Gradle consumer resolved the local Maven artifact and built ConsumerShared.framework.
- Xcode linked that framework into a Swift app with the public PushPort Apple SDK 0.0.1 and Notification Service Extension. This verifies the final Objective-C bridge linkage, not just Kotlin metadata compilation.

Evidence: [GitHub verification run](https://github.com/IamFromUA/PushPortLibraryKmp/actions/runs/34774169293). The bootstrap Android dependency used an explicit local repository built from a pinned public SDK revision. No hardcoded sibling checkout is required.

These checks do not register real applications, request permissions or send notifications. Device/browser integration and provider delivery checks are scheduled separately. Maven Central publication still requires publisher credentials; GitHub release artifacts are a separate distribution channel.
