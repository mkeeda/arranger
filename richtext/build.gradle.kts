plugins {
    id("arranger.android.library")
}

android {
    namespace = "dev.mkeeda.arranger.richtext"
}

dependencies {
    testImplementation(libs.junit)
    testImplementation(libs.kotest.assertions.core)
}
