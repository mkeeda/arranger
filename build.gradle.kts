plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.spotless) apply false
    alias(libs.plugins.dokka) apply false
}

allprojects {
    extra["org.jetbrains.dokka.experimental.gradle.pluginMode"] = "V2Enabled"
}

apply(plugin = "org.jetbrains.dokka")

dependencies {
    "dokka"(project(":richtext"))
    "dokka"(project(":richtext-editor"))
    "dokka"(project(":richtext-editor-material3"))
    "dokka"(project(":richtext-markdown"))
    "dokka"(project(":richtext-html"))
}

tasks.register("dokkaHtmlMultiModule") {
    dependsOn(tasks.named("dokkaGeneratePublicationHtml"))
    description = "Generates multi-module HTML documentation using Dokka"
    group = "documentation"
}