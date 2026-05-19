plugins {
    id("arranger.desktop.app")
    id("arranger.kmp.compose")
}

import org.jetbrains.compose.desktop.application.dsl.TargetFormat

kotlin {
    sourceSets {
        val jvmMain by getting {
            dependencies {
                implementation(project(":sample:shared"))
                implementation(compose.desktop.currentOs)
                implementation(compose.material3)
                implementation(compose.foundation)
                implementation(compose.ui)
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "dev.mkeeda.arranger.sample.desktop.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "ArrangerSample"
            macOS {
                bundleID = "dev.mkeeda.arranger.sample.desktop"
            }
        }
    }
}
