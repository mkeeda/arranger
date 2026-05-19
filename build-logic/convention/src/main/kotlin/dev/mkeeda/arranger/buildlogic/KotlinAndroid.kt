package dev.mkeeda.arranger.buildlogic

import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryExtension
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

internal fun Project.configureKotlinMultiplatformAndroid(
    extension: KotlinMultiplatformExtension,
) {
    (extension as ExtensionAware).extensions.configure("android", Action<KotlinMultiplatformAndroidLibraryExtension> {
        compileSdk = 37
        minSdk = 26
    })
    
    configureJavaToolchain()
    
    // Free compiler args for KMP targets
    extension.targets.configureEach {
        compilations.configureEach {
            compileTaskProvider.configure {
                compilerOptions {
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

internal fun Project.configureKotlinAndroid(
    commonExtension: CommonExtension,
) {
    commonExtension.apply {
        compileSdk = 37
    }

    configureKotlin()
}

private fun Project.configureKotlin() {
    configureJavaToolchain()

    // Common configurations for Kotlin compilation tasks
    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
            freeCompilerArgs.addAll(
                "-opt-in=kotlin.RequiresOptIn",
                "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
                "-opt-in=dev.mkeeda.arranger.richtext.InternalArrangerApi",
            )
        }
    }
}

private fun Project.configureJavaToolchain() {
    // Lock Kotlin/Java versions via Java Toolchain
    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }
}
