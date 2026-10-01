plugins {
    id("com.android.test")
}

// Keep generated device trace paths within Windows path limits.
layout.buildDirectory.set(rootProject.layout.projectDirectory.dir("build/bench/test"))

android {
    namespace = "br.com.arch.toolkit.statebenchmark.test"
    compileSdk {
        version = release(libs.versions.build.sdk.compile.get().toInt()) {
            minorApiLevel = libs.versions.build.sdk.minor.get().toInt()
        }
    }
    buildToolsVersion = libs.versions.build.tools.get()
    defaultConfig {
        minSdk = 29
        targetSdk = 37
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    targetProjectPath = ":state-benchmark-app"
    experimentalProperties["android.experimental.self-instrumenting"] = true
    buildTypes {
        create("release") {
            isDebuggable = true
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

dependencies {
    implementation("androidx.benchmark:benchmark-macro-junit4:1.5.0")
    implementation("androidx.test.uiautomator:uiautomator:2.3.0")
    implementation(libs.androidx.test.junit)
}
