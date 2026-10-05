plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.spotless) apply false
    alias(libs.plugins.dokka)
}

dependencies {
    "dokka"(project(":arranger-richtext"))
    "dokka"(project(":arranger-editor"))
    "dokka"(project(":arranger-editor-material3"))
    "dokka"(project(":arranger-markdown"))
    "dokka"(project(":arranger-html"))
}