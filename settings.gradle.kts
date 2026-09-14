pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        flatDir {
            dirs("build-logic/ktlint-rules/build/libs")
        }
    }
}

rootProject.name = "Arranger"
includeBuild("build-logic")
include(":sample:android")
include(":sample:desktop")
include(":sample:shared")
include(":sample:web")
include(":richtext")
include(":richtext-editor")
include(":richtext-editor-material3")
include(":richtext-markdown")
include(":richtext-html")
