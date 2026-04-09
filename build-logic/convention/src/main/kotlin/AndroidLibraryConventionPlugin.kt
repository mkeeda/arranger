import com.android.build.api.dsl.LibraryExtension
import dev.mkeeda.arranger.buildlogic.configureKotlinAndroid
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.library")
                apply("arranger.android.spotless")
            }

            extensions.configure<LibraryExtension> {
                configureKotlinAndroid(this)
                defaultConfig.minSdk = 26

                // ライブラリモジュール向けの共通Lint設定
                lint {
                    warningsAsErrors = true
                    abortOnError = true
                }
            }

        }
    }
}
