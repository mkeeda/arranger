import com.vanniktech.maven.publish.AndroidSingleVariantLibrary
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class MavenPublishConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.vanniktech.maven.publish")
            extensions.configure<MavenPublishBaseExtension> {
                publishToMavenCentral()
                // Dokka (K1 engine) fails to resolve opt-in annotation markers (e.g., @InternalArrangerApi)
                // used in richtext-editor, causing the javadoc generation task to crash.
                // JavadocJar.None() skips javadoc jar generation, which is acceptable because:
                //   - Maven Central (Central Portal) does not require a javadoc jar.
                //   - IDE quick-docs work fine via the sources jar.
                // TODO: Re-enable javadoc generation once migrating to Dokka K2 engine.
                configure(AndroidSingleVariantLibrary(javadocJar = JavadocJar.None()))
            }
        }
    }
}
