import io.github.takahirom.roborazzi.RoborazziExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType

class AndroidScreenshotTestConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("io.github.takahirom.roborazzi")

            pluginManager.withPlugin("com.android.library") {
                extensions.configure<com.android.build.api.dsl.LibraryExtension> {
                    testOptions {
                        unitTests {
                            isIncludeAndroidResources = true
                        }
                    }
                }
            }

            pluginManager.withPlugin("com.android.application") {
                extensions.configure<com.android.build.api.dsl.ApplicationExtension> {
                    testOptions {
                        unitTests {
                            isIncludeAndroidResources = true
                        }
                    }
                }
                extensions.configure<RoborazziExtension> {
                    outputDir.set(project.file("src/test/screenshots"))
                }
            }
            
            pluginManager.withPlugin("com.android.library") {
                extensions.configure<RoborazziExtension> {
                    outputDir.set(project.file("src/test/screenshots"))
                }
            }

            pluginManager.withPlugin("com.android.kotlin.multiplatform.library") {
                extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension> {
                    (this as org.gradle.api.plugins.ExtensionAware).extensions.configure("android", org.gradle.api.Action<com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryExtension> {
                        withHostTest {
                            isIncludeAndroidResources = true
                        }
                    })
                }
                extensions.configure<RoborazziExtension> {
                    outputDir.set(project.file("src/androidHostTest/screenshots"))
                }
            }

            val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
            pluginManager.withPlugin("com.android.application") {
                dependencies {
                    add("testImplementation", libs.findLibrary("roborazzi").get())
                    add("testImplementation", libs.findLibrary("roborazzi-compose").get())
                    add("testImplementation", libs.findLibrary("robolectric").get())
                    add("testImplementation", libs.findLibrary("androidx-compose-ui-test-junit4").get())
                    add("debugImplementation", libs.findLibrary("androidx-compose-ui-test-manifest").get())
                }
            }

            pluginManager.withPlugin("com.android.kotlin.multiplatform.library") {
                extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension> {
                    sourceSets.named("androidHostTest").configure {
                        dependencies {
                            implementation(libs.findLibrary("roborazzi").get())
                            implementation(libs.findLibrary("roborazzi-compose").get())
                            implementation(libs.findLibrary("robolectric").get())
                            implementation(libs.findLibrary("androidx-compose-ui-test-junit4").get())
                            implementation(libs.findLibrary("androidx-compose-ui-test-manifest").get())
                        }
                    }
                }
            }
        }
    }
}
