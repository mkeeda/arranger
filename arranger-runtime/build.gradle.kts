// Workaround for IntelliJ IDE bug https://youtrack.jetbrains.com/issue/KTIJ-19369
@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "dev.mkeeda.arranger.runtime"
}

dependencies {
    implementation(libs.androidx.compose.runtime)
}
