plugins {
    id("arranger.kmp.library")
    id("arranger.android.target")
    id("arranger.desktop.target")
    id("arranger.maven.publish")
}

kotlin {
    android {
        namespace = "dev.mkeeda.arranger.richtext"
        withHostTest {}
    }
    sourceSets {
        val androidHostTest by getting {
            dependencies {
                implementation(libs.junit)
                implementation(libs.kotest.assertions.core)
            }
        }
    }
}
