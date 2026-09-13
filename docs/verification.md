# Preparation checks — 2026-09-13

Passed on Windows/JDK 17 with Android SDK 36 and Node 24:

- Common behavior tests on JVM, Android host and Node.js (3 tests per target).
- Compilation of the Android and Kotlin/JS adapters.
- Local Maven publication of Android, JVM and JS artifacts with POM, sources and documentation.

The unpublished Android dependency was resolved through an explicit local Maven repository property, not a sibling source dependency. Root/iOS publication was not presented as complete: Cinterop needs the Apple toolchain on macOS. Final iOS linking with the native Swift package and an independent KMP consumer still must pass before release.

Nothing has been uploaded to Maven Central or GitHub. Local Maven output is for package verification only.
