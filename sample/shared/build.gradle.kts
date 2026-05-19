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
                api(project(":richtext-editor-material3"))
                api(project(":richtext-editor"))
                api(compose.components.resources)
                api(compose.components.uiToolingPreview)
                api(compose.foundation)
                api(compose.material3)
                api(compose.ui)
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
