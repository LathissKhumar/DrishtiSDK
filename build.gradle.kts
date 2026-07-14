plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.dokka) apply false
    alias(libs.plugins.kotlinx.binary.compatibility.validator) apply false
    id("com.vanniktech.maven.publish") version "0.34.0" apply false
}

allprojects {
    group = "io.github.lathisskhumar"
    version = "1.0.0"
}

subprojects {
    if (name == "drishti-demo" || name == "drishti-test") return@subprojects

    pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
        apply(plugin = "com.vanniktech.maven.publish")
    }
}

tasks.register("dokkaHtmlMultiModule", org.jetbrains.dokka.gradle.DokkaMultiModuleTask::class) {
    pluginsMapConfiguration.set(
        mapOf(
            "org.jetbrains.dokka.base.DokkaBase" to """{
                "footerMessage": "Copyright 2026 Drishti SDK Contributors"
            }"""
        )
    )
}
