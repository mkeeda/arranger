plugins {
    id("arranger.kmp.library")
    id("arranger.maven.publish")
    id("arranger.android.screenshot")
    alias(libs.plugins.kotlin.compose)
}

kotlin {
    android {
        namespace = "dev.mkeeda.arranger.richtext.editor"
    }
    sourceSets {
        val androidMain by getting {
            dependencies {
                api(project(":richtext"))
                api(project.dependencies.platform(libs.androidx.compose.bom))
                api(libs.androidx.compose.foundation)
            }
        }
        val androidHostTest by getting {
            dependencies {
                implementation(libs.junit)
                implementation(libs.kotest.assertions.core)
                implementation(libs.androidx.activity.compose)
            }
        }
    }
}
