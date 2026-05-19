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
        withHostTest {
            isIncludeAndroidResources = true
        }
    }
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":richtext-editor"))
                api(compose.material3)
            }
        }
        val androidHostTest by getting {
            dependencies {
                implementation(libs.junit)
                implementation(libs.kotest.assertions.core)
                implementation(libs.robolectric)
                implementation(libs.androidx.compose.ui.test.junit4)
                implementation(libs.androidx.compose.ui.test.manifest)
                implementation(libs.androidx.activity.compose)
            }
        }
    }
}
