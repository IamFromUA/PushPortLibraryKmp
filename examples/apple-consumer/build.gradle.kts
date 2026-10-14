plugins { kotlin("multiplatform") version "2.3.21" }

kotlin {
    iosSimulatorArm64 {
        binaries.framework {
            baseName = "ConsumerShared"
            isStatic = true
        }
    }
    sourceSets.commonMain.dependencies {
        implementation("dev.pushport:kmp-sdk:0.0.2")
    }
}
