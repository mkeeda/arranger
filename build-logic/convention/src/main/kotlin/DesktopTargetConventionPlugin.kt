import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class DesktopTargetConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            // Requires org.jetbrains.kotlin.multiplatform to be applied first
            // (provided by arranger.kmp.library)
            extensions.configure<KotlinMultiplatformExtension> {
                jvm()
            }
        }
    }
}
