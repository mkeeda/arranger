import com.android.build.api.dsl.ApplicationExtension
import dev.mkeeda.arranger.buildlogic.configureAndroidLint
import dev.mkeeda.arranger.buildlogic.configureKotlinAndroid
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.application")
                apply("arranger.android.spotless")
            }

            extensions.configure<ApplicationExtension> {
                configureKotlinAndroid(this)
                configureAndroidLint()
                defaultConfig.targetSdk = 37
                defaultConfig.minSdk = 26
                
                lint {
                    checkReleaseBuilds = false
                }
            }
        }
    }
}
