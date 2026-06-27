pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "drishti-sdk"

include(":drishti-core")
include(":drishti-vision")
include(":drishti-graph")
include(":drishti-formula")
include(":drishti-molecule")
include(":drishti-haptics")
include(":drishti-audio")
include(":drishti-voice")
include(":drishti-android")
include(":drishti-demo")
