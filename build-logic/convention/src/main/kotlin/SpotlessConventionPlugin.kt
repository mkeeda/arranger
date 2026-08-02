import com.diffplug.gradle.spotless.SpotlessExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType


class SpotlessConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.diffplug.spotless")

            val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
            val ktlintVersion = libs.findVersion("ktlint").get().toString()

            extensions.configure<SpotlessExtension> {
                kotlin {
                    target("**/*.kt")
                    targetExclude("**/build/**/*.kt")
                    ktlint(ktlintVersion).setEditorConfigPath(target.rootProject.file(".editorconfig"))
                }
                kotlinGradle {
                    target("**/*.kts")
                    targetExclude("**/build/**/*.kts")
                    ktlint(ktlintVersion).setEditorConfigPath(target.rootProject.file(".editorconfig"))
                }
                format("xml") {
                    target("**/*.xml")
                    targetExclude("**/build/**/*.xml")
                }
            }

            // Wire the custom rule as a separate task to avoid Spotless Configuration Cache serialization bug
            val checkFullyQualifiedNames = tasks.register("checkFullyQualifiedNames") {
                val kotlinFiles = fileTree("src") {
                    include("**/*.kt")
                    exclude("**/build/**")
                }
                inputs.files(kotlinFiles)
                val outputFile = layout.buildDirectory.file("reports/checkFullyQualifiedNames/success.txt")
                outputs.file(outputFile)
                
                doLast {
                    kotlinFiles.forEach { file ->
                        dev.mkeeda.arranger.buildlogic.SpotlessCustomRules.noFullyQualifiedNames(file.readText())
                    }
                    outputFile.get().asFile.apply {
                        parentFile.mkdirs()
                        writeText("Success")
                    }
                }
            }

            tasks.configureEach {
                if (name == "spotlessCheck") {
                    dependsOn(checkFullyQualifiedNames)
                }
            }
        }
    }
}
