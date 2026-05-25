package dev.mkeeda.arranger.buildlogic

import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

internal fun Project.configureKmpCommonOptions(
    extension: KotlinMultiplatformExtension,
) {
    configureJavaToolchain()

    // Free compiler args for KMP targets
    extension.targets.configureEach {
        compilations.configureEach {
            compileTaskProvider.configure {
                compilerOptions {
                    allWarningsAsErrors.set(true)
                    freeCompilerArgs.addAll(
                        "-opt-in=kotlin.RequiresOptIn",
                        "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
                        "-opt-in=dev.mkeeda.arranger.richtext.InternalArrangerApi",
                    )
                }
            }
        }
    }
}

internal fun Project.configureKotlin() {
    configureJavaToolchain()

    // Common configurations for Kotlin compilation tasks
    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
            allWarningsAsErrors.set(true)
            freeCompilerArgs.addAll(
                "-opt-in=kotlin.RequiresOptIn",
                "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
                "-opt-in=dev.mkeeda.arranger.richtext.InternalArrangerApi",
            )
        }
    }
}

internal fun Project.configureJavaToolchain() {
    // Lock Kotlin/Java versions via Java Toolchain
    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }
}
