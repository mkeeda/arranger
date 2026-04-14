plugins {
    id("arranger.android.library")
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "dev.mkeeda.arranger.ui"
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
    implementation(project(":core"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.foundation)

    testImplementation(libs.junit)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.activity.compose)
    testImplementation(libs.robolectric)
}
