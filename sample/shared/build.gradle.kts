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
                implementation(compose.components.resources)
                implementation(compose.components.uiToolingPreview)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
                @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
                implementation(compose.uiTest)
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
            }
        }
    }
}
