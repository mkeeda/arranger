plugins {
    id("arranger.android.library")
    id("arranger.maven.publish")
    id("arranger.android.screenshot")
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "dev.mkeeda.arranger.richtext.editor"
    buildFeatures {
        compose = true
    }
}

dependencies {
    api(project(":richtext"))

    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.foundation)

    testImplementation(libs.junit)
    testImplementation(libs.kotest.assertions.core)
    debugImplementation(libs.androidx.activity.compose)
}
