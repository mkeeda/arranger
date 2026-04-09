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
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "arranger.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = "arranger.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidSpotless") {
            id = "arranger.android.spotless"
            implementationClass = "AndroidSpotlessConventionPlugin"
        }
    }
}
