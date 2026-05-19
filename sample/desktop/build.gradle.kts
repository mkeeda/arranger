plugins {
    id("arranger.desktop.app")
    id("arranger.kmp.compose")
}

kotlin {
    sourceSets {
        val jvmMain by getting {
            dependencies {
                implementation(project(":sample:shared"))
                implementation(compose.desktop.currentOs)
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "dev.mkeeda.arranger.sample.desktop.MainKt"
        nativeDistributions {
            targetFormats(org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg, org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi, org.jetbrains.compose.desktop.application.dsl.TargetFormat.Deb)
            packageName = "ArrangerSample"
            macOS {
                bundleID = "dev.mkeeda.arranger.sample.desktop"
            }
        }
    }
}
