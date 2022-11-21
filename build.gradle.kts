import com.android.build.api.dsl.LibraryExtension

plugins {
    id("com.android.application") version "8.0.0-alpha08" apply false
    id("com.android.library") version "8.0.0-alpha08" apply false
    id("org.jetbrains.kotlin.android") version "1.7.20" apply false
}

// TODO Declare plugin
subprojects {
    plugins.withType<com.android.build.gradle.LibraryPlugin> {
        configure<LibraryExtension> {
            compileSdk = libs.versions.compileSdk.get().toInt()

            defaultConfig {
                minSdk = libs.versions.minSdk.get().toInt()
                testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            }

            buildTypes {
                release {
                    isMinifyEnabled = false
                }
            }
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_1_8
                targetCompatibility = JavaVersion.VERSION_1_8
            }
        }
    }
}
