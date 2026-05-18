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
                extensions.configure<com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryExtension>("kotlinMultiplatformAndroidLibrary") {
                    withHostTest {
                        isIncludeAndroidResources = true
                    }
                }
                extensions.configure<RoborazziExtension> {
                    outputDir.set(project.file("src/androidHostTest/screenshots"))
                }
            }

            val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
            dependencies {
                "testImplementation"(libs.findLibrary("roborazzi").get())
                "testImplementation"(libs.findLibrary("roborazzi-compose").get())
                "testImplementation"(libs.findLibrary("robolectric").get())
                "testImplementation"(libs.findLibrary("androidx-compose-ui-test-junit4").get())
                "debugImplementation"(libs.findLibrary("androidx-compose-ui-test-manifest").get())
            }
        }
    }
}
