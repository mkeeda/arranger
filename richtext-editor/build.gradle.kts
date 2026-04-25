plugins {
    id("arranger.android.library")
    id("arranger.maven.publish")
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "dev.mkeeda.arranger.richtext.editor"
    buildFeatures {
        compose = true
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    api(project(":richtext"))

    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.foundation)

    testImplementation(libs.junit)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.activity.compose)
    testImplementation(libs.robolectric)
}
