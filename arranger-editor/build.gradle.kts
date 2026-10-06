plugins {
    id("arranger.kmp.library")
    id("arranger.android.target")
    id("arranger.desktop.target")
    id("arranger.ios.target")
    id("arranger.wasmjs.target")
    id("arranger.kmp.compose")
    id("arranger.maven.publish")
    id("arranger.android.screenshot")
    alias(libs.plugins.dokka)
}

kotlin {
    android {
        namespace = "dev.mkeeda.arranger.editor"
    }
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":arranger-richtext"))
                api(libs.jetbrains.compose.foundation)
                api(libs.jetbrains.compose.ui)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotest.assertions.core)
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
        val androidHostTest by getting {
            dependencies {
                implementation(libs.androidx.activity.compose)
            }
        }
    }
}
