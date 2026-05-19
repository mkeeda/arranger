import dev.mkeeda.arranger.buildlogic.configureAndroidLint
import dev.mkeeda.arranger.buildlogic.configureAndroidTarget
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class AndroidTargetConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.kotlin.multiplatform.library")
            }

            extensions.configure<KotlinMultiplatformExtension> {
                configureAndroidTarget(this)
            }

            configureAndroidLint()
        }
    }
}
