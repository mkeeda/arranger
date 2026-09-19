plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.spotless) apply false
    alias(libs.plugins.dokka)
}

dependencies {
    "dokka"(project(":richtext"))
    "dokka"(project(":richtext-editor"))
    "dokka"(project(":richtext-editor-material3"))
    "dokka"(project(":richtext-markdown"))
    "dokka"(project(":richtext-html"))
}