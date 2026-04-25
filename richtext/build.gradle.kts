plugins {
    id("arranger.android.library")
    id("arranger.maven.publish")
}

android {
    namespace = "dev.mkeeda.arranger.richtext"
}

dependencies {
    testImplementation(libs.junit)
    testImplementation(libs.kotest.assertions.core)
}
