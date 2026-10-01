plugins {
    id("com.android.application")
    alias(libs.plugins.jetbrains.serialization)
}

// Keep generated device trace paths within Windows path limits.
layout.buildDirectory.set(rootProject.layout.projectDirectory.dir("build/bench/app"))

android {
    namespace = "br.com.arch.toolkit.statebenchmark"
    compileSdk {
        version = release(libs.versions.build.sdk.compile.get().toInt()) {
            minorApiLevel = libs.versions.build.sdk.minor.get().toInt()
        }
    }
    buildToolsVersion = libs.versions.build.tools.get()
    defaultConfig {
        applicationId = "br.com.arch.toolkit.statebenchmark"
        minSdk = 23
        targetSdk = 37
        versionCode = 1
        versionName = "1"
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

dependencies {
    implementation(project(":event-observer-state"))
    implementation("androidx.activity:activity-ktx:1.13.0")
    implementation(libs.androidx.lifecycle.runtime)
}
