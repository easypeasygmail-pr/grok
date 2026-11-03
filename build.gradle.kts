buildscript {
    dependencies {
        // AGP 9 ships an older Kotlin; pin the version the version catalog uses.
        classpath(libs.kotlin.gradle.plugin)
        classpath(libs.ksp.gradle.plugin)
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.room) apply false
    alias(libs.plugins.detekt) apply false
}

val ktlintCli =
    configurations.create("ktlintCli") {
        attributes {
            attribute(
                org.gradle.api.attributes.Bundling.BUNDLING_ATTRIBUTE,
                objects.named(
                    org.gradle.api.attributes.Bundling::class.java,
                    org.gradle.api.attributes.Bundling.SHADOWED,
                ),
            )
        }
    }

dependencies {
    ktlintCli(libs.ktlint.cli)
}

subprojects {
    apply(plugin = "dev.detekt")
    extensions.configure<dev.detekt.gradle.extensions.DetektExtension> {
        buildUponDefaultConfig = true
        config.setFrom(files("$rootDir/detekt.yml"))
        parallel = true
    }
    tasks.withType<dev.detekt.gradle.Detekt>().configureEach {
        jvmTarget = "21"
        exclude("**/build/**")
    }
}

tasks.register<JavaExec>("ktlintCheck") {
    group = "verification"
    description = "Checks Kotlin style with ktlint (io.github.ktlint.core)."
    classpath = ktlintCli
    mainClass.set("io.github.ktlint.core.Main")
    workingDir = rootDir
    args(
        "**/src/**/*.kt",
        "**/*.kts",
        "!**/build/**",
        "!third_party/**",
    )
}

tasks.register<JavaExec>("ktlintFormat") {
    group = "formatting"
    description = "Formats Kotlin sources with ktlint (io.github.ktlint.core)."
    classpath = ktlintCli
    mainClass.set("io.github.ktlint.core.Main")
    workingDir = rootDir
    args(
        "--format",
        "**/src/**/*.kt",
        "**/*.kts",
        "!**/build/**",
        "!third_party/**",
    )
}
