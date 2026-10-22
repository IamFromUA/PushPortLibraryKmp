# Publishing to Maven Central

Repository: https://github.com/IamFromUA/PushPortLibraryKmp. Coordinate: `dev.pushport:kmp-sdk:0.0.2`. Version 0.0.2 and its native Android dependency are published on Maven Central. Publish future versions after their native Android dependency is public. The iOS host links [PushPort 0.0.2 through SPM](https://github.com/IamFromUA/PushPortLibraryApple); Kotlin/JS hosts install the compatible Web package from its public GitHub archive while npm registry publication remains pending.

## Complete release verification

Use macOS, Xcode, JDK 17, Node 24 and Android SDK 36. Run `./gradlew jvmTest jsNodeTest compileAndroidMain testAndroidHostTest publishAllPublicationsToLocalReleaseRepository`, then `bash scripts/verify-apple-consumer.sh "$PWD/build/maven-repository"`. The consumer resolves the Maven KLIB from a separate Gradle project, builds a static framework and links it into a Swift app with the public Swift package. It never registers an application or sends notifications.

The complete repository contains root metadata and Android, iOS arm64, iOS simulator arm64/x64, JS and JVM target publications, sources, documentation, POM metadata and notices. Do not publish a partial Windows/Linux build as the full KMP release. CI's explicit Android repository override is only a bootstrap mechanism before the initial Maven Central release; consumers use normal dependency resolution after publication.

## Credentials and workflow

The Central account must own the verified namespace `dev.pushport`. Generate its publishing User Token in the portal. Keep a protected PGP signing key and publish only its public part to a supported keyserver. In the GitHub **maven-central** environment configure:

| Secret | Value |
| --- | --- |
| MAVEN_CENTRAL_USERNAME | Central publishing token username |
| MAVEN_CENTRAL_PASSWORD | Central publishing token password |
| SIGNING_KEY | ASCII-armored private PGP signing key |
| SIGNING_KEY_ID | Last eight hexadecimal digits of its key ID |
| SIGNING_PASSWORD | PGP private-key passphrase |

Create an immutable Git tag matching `version` in build.gradle.kts. Run **Publish Maven Central** manually with that tag. It verifies all target artifacts and final Swift linking, signs in memory, and uploads/releases via the Central Portal API. The private key is not stored in repository files or artifacts. Ordinary pushes do not publish. A successful upload may take time to appear in Maven mirrors.

After publication, verify an independent consumer using only `google()` and `mavenCentral()`. Fixes require a new version; never overwrite `0.0.2`.

References: [Kotlin Maven publication](https://kotlinlang.org/docs/multiplatform/multiplatform-publish-libraries-to-maven.html), [Central requirements](https://central.sonatype.org/publish/requirements/).

## Publisher policy

From 2026-10-01 Sonatype requires Publisher Pro for artifacts supporting commercial products or services. Review the [current policy](https://central.sonatype.org/news/20260908_publisher_tiers_commercial_use/) before future commercial PushPort releases; a public repository or open-source license alone does not determine eligibility.
