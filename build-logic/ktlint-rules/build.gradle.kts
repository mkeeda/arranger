plugins {
    `kotlin-dsl`
}

group = "dev.mkeeda.arranger.ktlint"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

dependencies {
    compileOnly(libs.ktlint.rule.engine.core)
    compileOnly(libs.ktlint.cli.ruleset.core)

    testImplementation(libs.ktlint.rule.engine.core)
    testImplementation(libs.ktlint.cli.ruleset.core)
    testImplementation(libs.ktlint.test)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotest.assertions.core)
}
