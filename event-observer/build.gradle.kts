plugins {
    id("arch-multi-library")
    id("arch-lint")
    id("arch-documentation")
    id("arch-optimize")
    id("arch-publish")
    alias(libs.plugins.jetbrains.atomic)
}

kotlin {

    android {
        compileSdk = versionInt(libs.versions.build.sdk.compile)
        minSdk = versionInt(libs.versions.build.sdk.min)
        buildToolsVersion = versionString(libs.versions.build.tools)
    }

    sourceSets {
        // Libraries
        commonMain.dependencies {
            implementation(libs.jetbrains.coroutines.core)
            implementation(libs.androidx.lifecycle.runtime)
        }
        androidMain.dependencies {
            implementation(libs.androidx.lifecycle.livedata)
        }

        // Test Libraries
        javaTest.dependencies {
            implementation(libs.jetbrains.coroutines.test)
            implementation(libs.jetbrains.kotlin.test)
            implementation(libs.mockk.test.agent)
        }
        androidHostTest.dependencies {
            implementation(libs.mockk.test.android)
            implementation(libs.androidx.arch.coreTesting)
        }
    }
}

dokka.dokkaSourceSets.configureEach {
    sourceLink {
        localDirectory.set(projectDir.resolve("src"))
        remoteUrl("${env("POM_URL")}/tree/master/event-observer/src")
    }
}
