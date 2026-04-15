plugins {
    id("arch-multi-library")
    id("arch-lint")
    id("arch-documentation")
    id("arch-optimize")
    id("arch-publish")
    alias(libs.plugins.jetbrains.atomic)
    alias(libs.plugins.jetbrains.compose.compiler)
}

kotlin {
    android {
        compileSdk = versionInt(libs.versions.build.sdk.compile)
        minSdk = versionInt(libs.versions.build.sdk.min)
        buildToolsVersion = versionString(libs.versions.build.tools)
    }

    // Libraries
    sourceSets.commonMain.dependencies {
        implementation(project(":event-observer"))

        implementation(libs.jetbrains.coroutines.core)
        implementation(libs.androidx.compose.lifecycle)
        implementation(libs.jetbrains.compose.runtime)
        implementation(libs.jetbrains.compose.animation)
    }
    sourceSets.androidMain.dependencies {
        implementation(libs.androidx.lifecycle.livedata)
    }

    // Test Libraries
    sourceSets.commonTest.dependencies {
        implementation(libs.jetbrains.kotlin.test)
        implementation(project(":test"))
        implementation(libs.jetbrains.coroutines.test)
        implementation(libs.jetbrains.compose.foundation)
        implementation(libs.jetbrains.compose.ui.test)
    }
    sourceSets.jvmTest.dependencies {
        implementation(libs.jetbrains.compose.desktop)
        implementation(libs.jetbrains.compose.ui.test.junit4.desktop)
    }
}
