plugins {
    id("arch-multi-library")
    id("arch-lint")
    id("arch-documentation")
    id("arch-optimize")
    id("arch-publish")
    id("kotlin-parcelize")
    kotlin("plugin.serialization") version "2.4.10"
}

kotlin {
    android {
        compileSdk = versionInt(libs.versions.build.sdk.compile)
        minSdk = versionInt(libs.versions.build.sdk.min)
        buildToolsVersion = versionString(libs.versions.build.tools)
    }
    sourceSets {
        commonMain.dependencies {
            api(project(":event-observer"))
            api("androidx.lifecycle:lifecycle-viewmodel-savedstate:2.11.0")
            api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
            implementation(libs.jetbrains.coroutines.core)
            implementation("io.github.matheus-corregiari:arch-lumber:1.4.0")
            implementation("io.insert-koin:koin-core:4.2.2")
        }
        commonTest.dependencies {
            implementation(libs.jetbrains.kotlin.test)
            implementation(libs.jetbrains.coroutines.test)
        }
    }
}

