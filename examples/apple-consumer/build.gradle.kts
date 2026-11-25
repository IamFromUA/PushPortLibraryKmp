plugins { kotlin("multiplatform") version "2.3.21" }

kotlin {
    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "ConsumerShared"
            isStatic = true
        }
    }
    sourceSets.commonMain.dependencies {
        implementation("dev.pushport:kmp-sdk:0.1.1")
    }
}
