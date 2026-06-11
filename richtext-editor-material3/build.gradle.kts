
plugins {
    id("arranger.kmp.library")
    id("arranger.android.target")
    id("arranger.desktop.target")
    id("arranger.ios.target")
    id("arranger.kmp.compose")
    id("arranger.maven.publish")
}

kotlin {
    android {
        namespace = "dev.mkeeda.arranger.richtext.editor.material3"
    }
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":richtext-editor"))
                api(libs.jetbrains.compose.material3)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotest.assertions.core)
                @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
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
