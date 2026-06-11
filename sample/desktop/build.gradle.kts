
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    id("arranger.desktop.app")
    id("arranger.kmp.compose")
}

kotlin {
    sourceSets {
        val jvmMain by getting {
            dependencies {
                implementation(project(":sample:shared"))
                // TODO: Remove and use single desktop dependency once CMP-9175 is resolved
                // https://youtrack.jetbrains.com/issue/CMP-9175/Introduce-a-single-desktop-dependency-for-all-platforms
                implementation(compose.desktop.currentOs)
                implementation(libs.jetbrains.compose.material3)
                implementation(libs.jetbrains.compose.foundation)
                implementation(libs.jetbrains.compose.ui)
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
                @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
                implementation(libs.jetbrains.compose.uiTest)
                implementation(compose.desktop.uiTestJUnit4)
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
