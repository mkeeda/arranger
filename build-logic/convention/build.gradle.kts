plugins {
    `kotlin-dsl`
}

group = "dev.mkeeda.arranger.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.spotless.gradlePlugin)
    implementation(libs.vanniktech.maven.publish.gradlePlugin)
    implementation(libs.roborazzi.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "arranger.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("kmpLibrary") {
            id = "arranger.kmp.library"
            implementationClass = "KmpLibraryConventionPlugin"
        }
        register("spotless") {
            id = "arranger.spotless"
            implementationClass = "SpotlessConventionPlugin"
        }
        register("mavenPublish") {
            id = "arranger.maven.publish"
            implementationClass = "MavenPublishConventionPlugin"
        }
        register("androidScreenshotTest") {
            id = "arranger.android.screenshot"
            implementationClass = "AndroidScreenshotTestConventionPlugin"
        }
    }
}
