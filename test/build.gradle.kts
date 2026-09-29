plugins {
    id("arch-multi-library")
    id("arch-lint")
    id("arch-optimize")
}

kotlin {
    android {
        compileSdk {
            version = release(versionInt(libs.versions.build.sdk.compile)) {
                minorApiLevel = versionInt(libs.versions.build.sdk.minor)
            }
        }
        minSdk = versionInt(libs.versions.build.sdk.min)
        buildToolsVersion = versionString(libs.versions.build.tools)
    }
    sourceSets {
        // Libraries
        commonMain.dependencies {
            api(libs.jetbrains.kotlin.test)
        }
        androidMain.dependencies {
            api(libs.androidx.compose.testManifest)
            api(libs.androidx.test.junit)
            api(libs.robolectric.test)
        }
    }
}

tasks.withType<AbstractTestTask>().configureEach { failOnNoDiscoveredTests = false }
