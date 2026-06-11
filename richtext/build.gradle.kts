plugins {
    id("arranger.kmp.library")
    id("arranger.android.target")
    id("arranger.desktop.target")
    id("arranger.ios.target")
    id("arranger.maven.publish")
}

kotlin {
    android {
        namespace = "dev.mkeeda.arranger.richtext"
    }
    sourceSets {
        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotest.assertions.core)
            }
        }
    }
}
