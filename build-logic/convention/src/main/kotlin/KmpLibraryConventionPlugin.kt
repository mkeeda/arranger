import dev.mkeeda.arranger.buildlogic.configureKmpCommonOptions
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class KmpLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("org.jetbrains.kotlin.multiplatform")
                apply("arranger.spotless")
            }

            extensions.configure<KotlinMultiplatformExtension> {
                explicitApi()
                configureKmpCommonOptions(this)
            }
        }
    }
}
