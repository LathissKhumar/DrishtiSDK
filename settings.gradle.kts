pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "drishti-sdk"

include(":drishti-core")
include(":drishti-test")
include(":drishti-vision")
include(":drishti-graph")
include(":drishti-formula")
include(":drishti-molecule")
include(":drishti-haptics")
include(":drishti-audio")
include(":drishti-voice")
include(":drishti-android")
include(":drishti-demo")
