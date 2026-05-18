import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryExtension
import dev.mkeeda.arranger.buildlogic.configureAndroidLint
import dev.mkeeda.arranger.buildlogic.configureKotlinMultiplatformAndroid
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class KmpLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.kotlin.multiplatform.library")
                apply("org.jetbrains.kotlin.multiplatform")
                apply("arranger.android.spotless")
            }

            extensions.configure<KotlinMultiplatformExtension> {
                explicitApi()
                configureKotlinMultiplatformAndroid(this)
            }

            configureAndroidLint()
        }
    }
}
