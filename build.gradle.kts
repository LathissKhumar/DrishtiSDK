plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.dokka) apply false
    alias(libs.plugins.kotlinx.binary.compatibility.validator) apply false
}

allprojects {
    group = "io.drishti"
    version = "1.0.0"
}

// Configure maven-publish for all library modules (excludes demo and test)
subprojects {
    if (name != "drishti-demo" && name != "drishti-test") {
        pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
            apply(plugin = "maven-publish")

            extensions.configure<PublishingExtension> {
                repositories {
                    mavenLocal()
                }
                publications.withType<MavenPublication>().configureEach {
                    artifactId = "drishti-${this@subprojects.name.removePrefix("drishti-")}"

                    pom {
                        name.set("Drishti ${this@subprojects.name.removePrefix("drishti-").replaceFirstChar { it.uppercase() }}")
                        description.set("Accessibility infrastructure for visual STEM content")
                        url.set("https://github.com/LathissKhumar/DrishtiSTEM")
                        licenses {
                            license {
                                name.set("The Apache License, Version 2.0")
                                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                            }
                        }
                        developers {
                            developer {
                                id.set("LathissKhumar")
                                name.set("Lathiss")
                                url.set("https://github.com/LathissKhumar")
                            }
                        }
                        scm {
                            connection.set("scm:git:https://github.com/LathissKhumar/DrishtiSTEM.git")
                            developerConnection.set("scm:git:ssh://github.com/LathissKhumar/DrishtiSTEM.git")
                            url.set("https://github.com/LathissKhumar/DrishtiSTEM")
                        }
                    }
                }
            }
        }
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
