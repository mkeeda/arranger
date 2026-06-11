
plugins {
    kotlin("multiplatform")
    id("arranger.android.target")
    id("arranger.desktop.target")
    id("arranger.kmp.compose")
}

kotlin {
    android {
        namespace = "dev.mkeeda.arranger.sample.shared"
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":richtext-editor-material3"))
                implementation(project(":richtext-editor"))
                implementation(libs.jetbrains.compose.components.resources)
                implementation(libs.jetbrains.compose.components.uiToolingPreview)
                implementation(libs.jetbrains.compose.foundation)
                implementation(libs.jetbrains.compose.material3)
                implementation(libs.jetbrains.compose.ui)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.jetbrains.compose.uiTest)
            }
        }
        val jvmTest by getting {
            dependencies {
                // TODO: Remove and use single desktop dependency once CMP-9175 is resolved
                // https://youtrack.jetbrains.com/issue/CMP-9175/Introduce-a-single-desktop-dependency-for-all-platforms
                implementation(compose.desktop.currentOs)
            }
        }
    }
}
