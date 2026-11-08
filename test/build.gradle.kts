plugins {
    id("arch-multi-library")
    id("arch-lint")
    id("arch-optimize")
}

kotlin {
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
