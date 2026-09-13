import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("multiplatform") version "2.3.21"
    kotlin("plugin.serialization") version "2.3.21"
    id("com.android.kotlin.multiplatform.library") version "9.2.1"
    id("com.vanniktech.maven.publish.base") version "0.37.0"
    `maven-publish`
    signing
}
group = "dev.pushport"
version = "0.0.1"

// CI and local development use an explicitly installed Node runtime (see README).
plugins.withType<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin> {
    the<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsEnvSpec>().download = false
}

kotlin {
    explicitApi()
    android {
        namespace = "dev.pushport.kmp"
        compileSdk = 36
        minSdk = 23
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
        withHostTestBuilder {}.configure {}
    }
    jvm { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }
    js(IR) { browser(); nodejs() }
    listOf(iosArm64(), iosSimulatorArm64(), iosX64()).forEach { target ->
        target.compilations.getByName("main").cinterops.create("PushPortBridge") {
            definitionFile.set(project.file("src/nativeInterop/cinterop/PushPortBridge.def"))
            includeDirs("src/nativeInterop/cinterop/include")
        }
    }
    sourceSets {
        commonMain.dependencies {
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
        }
        androidMain.dependencies { implementation("dev.pushport:android-sdk:0.0.1") }
    }
}

val docsJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
    from("docs", "README.md")
}
publishing {
    publications.withType<MavenPublication>().configureEach {
        artifact(docsJar)
        pom {
            name.set("PushPort Kotlin Multiplatform SDK")
            description.set("Common Kotlin API over the native PushPort Android, Apple and Web SDKs")
            url.set("https://pushport.dev")
            licenses { license { name.set("Apache License, Version 2.0"); url.set("https://www.apache.org/licenses/LICENSE-2.0.txt") } }
            developers { developer { id.set("IamFromUA"); name.set("Oleh Yurkov"); email.set("support@pushport.dev") } }
            scm {
                url.set("https://github.com/IamFromUA/PushPortLibraryKmp")
                connection.set("scm:git:https://github.com/IamFromUA/PushPortLibraryKmp.git")
                developerConnection.set("scm:git:ssh://git@github.com/IamFromUA/PushPortLibraryKmp.git")
            }
            inceptionYear.set("2026")
            issueManagement { system.set("GitHub"); url.set("https://github.com/IamFromUA/PushPortLibraryKmp/issues") }
        }
    }
    repositories { maven { name = "localRelease"; url = uri(layout.buildDirectory.dir("maven-repository")) } }
}
mavenPublishing {
    publishToMavenCentral()
    if (providers.gradleProperty("signingInMemoryKey").isPresent) signAllPublications()
}
tasks.withType<AbstractArchiveTask>().configureEach { isPreserveFileTimestamps = false; isReproducibleFileOrder = true }
tasks.withType<Jar>().configureEach { from(files("LICENSE", "NOTICE")) { into("META-INF/dev.pushport/kmp-sdk") } }
