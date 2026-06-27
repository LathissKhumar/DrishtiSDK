plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.application)
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "11"
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":drishti-core"))
                implementation(project(":drishti-haptics"))
                implementation(project(":drishti-audio"))
                implementation(project(":drishti-voice"))
                implementation(project(":drishti-graph"))
                implementation(project(":drishti-formula"))
                implementation(project(":drishti-molecule"))
                implementation(project(":drishti-vision"))
                implementation(libs.kotlinx.coroutines.core)
            }
        }
        val androidMain by getting {
            dependencies {
                implementation(libs.camerax.core)
                implementation(libs.camerax.camera2)
                implementation(libs.camerax.lifecycle)
                implementation(libs.camerax.view)
            }
        }
    }
}

android {
    namespace = "io.drishti.demo"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.drishti.demo"
        minSdk = 30
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
