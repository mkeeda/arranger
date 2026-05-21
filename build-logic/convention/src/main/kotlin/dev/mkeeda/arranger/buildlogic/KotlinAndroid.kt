package dev.mkeeda.arranger.buildlogic

import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryExtension
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

internal fun Project.configureAndroidTarget(
    extension: KotlinMultiplatformExtension,
) {
    (extension as ExtensionAware).extensions.configure("android", Action<KotlinMultiplatformAndroidLibraryExtension> {
        compileSdk = 37
        minSdk = 26
        @Suppress("UnstableApiUsage")
        experimentalProperties["android.experimental.kmp.enableAndroidResources"] = true
    })
}

internal fun Project.configureKotlinAndroid(
    commonExtension: CommonExtension,
) {
    commonExtension.apply {
        compileSdk = 37
    }

    configureKotlin()
}

