package dev.mkeeda.arranger.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

internal fun Project.configureKotlinMultiplatformAndroid(
    extension: KotlinMultiplatformExtension,
) {
    extensions.configure<KotlinMultiplatformAndroidLibraryExtension>("kotlinMultiplatformAndroidLibrary") {
        compileSdk = 37
        minSdk = 26
    }
    
    // Lock Kotlin/Java versions via Java Toolchain
    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(org.gradle.jvm.toolchain.JavaLanguageVersion.of(21))
        }
    }
    
    // Free compiler args for KMP android target
    extension.targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget>().configureEach {
        compilations.configureEach {
            compileTaskProvider.configure {
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
    // Lock Kotlin/Java versions via Java Toolchain
    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(org.gradle.jvm.toolchain.JavaLanguageVersion.of(21))
        }
    }

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
