pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // Optional, explicit local artifact repository for pre-publication verification.
        providers.gradleProperty("pushport.androidRepository").orNull?.let { path ->
            exclusiveContent {
                forRepository { maven { url = uri(path) } }
                filter { includeModule("dev.pushport", "android-sdk") }
            }
        }
        google(); mavenCentral()
    }
}
rootProject.name = "kmp-sdk"
