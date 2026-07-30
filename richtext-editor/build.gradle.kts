plugins {
    id("arranger.kmp.library")
    id("arranger.android.target")
    id("arranger.desktop.target")
    id("arranger.ios.target")
    id("arranger.wasmjs.target")
    id("arranger.kmp.compose")
    id("arranger.maven.publish")
    id("arranger.android.screenshot")
}

kotlin {
    android {
        namespace = "dev.mkeeda.arranger.richtext.editor"
    }
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":richtext"))
                api(libs.jetbrains.compose.foundation)
                api(libs.jetbrains.compose.ui)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotest.assertions.core)
            }
        }
        val androidHostTest by getting {
            dependencies {
                implementation(libs.androidx.activity.compose)
            }
        }
    }
}
