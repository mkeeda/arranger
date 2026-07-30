import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
    kotlin("multiplatform")
    id("arranger.wasmjs.target")
    id("arranger.kmp.compose")
}

kotlin {
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        outputModuleName.set("sample-web")
        browser {
            val projectDirPath = project.projectDir.path
            commonWebpackConfig {
                outputFileName = "sample-web.js"
                devServer = (devServer ?: KotlinWebpackConfig.DevServer()).apply {
                    static(projectDirPath)
                }
            }
        }
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":sample:shared"))
            implementation(libs.jetbrains.compose.ui)
            implementation(libs.jetbrains.compose.foundation)
            implementation(libs.jetbrains.compose.material3)
        }
    }
}
