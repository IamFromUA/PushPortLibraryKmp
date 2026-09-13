pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement {
    repositories {
        providers.gradleProperty("pushport.repository").orNull?.let { repository ->
            maven { url = uri(repository); content { includeGroup("dev.pushport") } }
        }
        google(); mavenCentral()
    }
}
rootProject.name = "pushport-apple-consumer"
