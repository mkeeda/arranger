package dev.mkeeda.arranger.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.dsl.Lint
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

internal fun Project.configureAndroidLint() {
    val configureLint: Lint.() -> Unit = {
        warningsAsErrors = true
        abortOnError = true
        
        // Prevent CI failures caused by new version availability warnings
        disable += setOf(
            "AndroidGradlePluginVersion",
            "GradleDependency"
        )
    }

    pluginManager.withPlugin("com.android.application") {
        extensions.configure<ApplicationExtension> {
            lint(configureLint)
        }
    }

    pluginManager.withPlugin("com.android.kotlin.multiplatform.library") {
        extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension> {
            (this as org.gradle.api.plugins.ExtensionAware).extensions.configure("android", org.gradle.api.Action<com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryExtension> {
                lint(configureLint)
            })
        }
    }
}
