plugins {
    id("arranger.kmp.library")
    id("arranger.android.target")
    id("arranger.desktop.target")
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
                api(compose.material3)
            }
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotest.assertions.core)
            @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
            implementation(compose.uiTest)
        }
        val jvmTest by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
            }
        }
    }
}
