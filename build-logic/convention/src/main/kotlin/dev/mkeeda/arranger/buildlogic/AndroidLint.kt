package dev.mkeeda.arranger.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryExtension
import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.dsl.Lint
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

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
        extensions.configure<KotlinMultiplatformExtension> {
            (this as ExtensionAware).extensions.configure("android", Action<KotlinMultiplatformAndroidLibraryExtension> {
                lint(configureLint)
            })
        }
    }
}
