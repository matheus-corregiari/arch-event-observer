plugins {
    id("arch-multi-library")
    id("arch-lint")
    id("arch-documentation")
    id("arch-optimize")
    id("arch-publish")
    alias(libs.plugins.jetbrains.atomic)
}


kotlin {
    androidLibrary {
        compileSdk = versionInt(libs.versions.build.sdk.compile)
        minSdk = versionInt(libs.versions.build.sdk.min)
        buildToolsVersion = versionString(libs.versions.build.tools)
    }
    // Libraries
//    sourceSets.commonMain.dependencies {
//        implementation(libs.jetbrains.coroutines.core)
//        implementation(libs.androidx.lifecycle.runtime)
//    }
//    sourceSets.androidMain.dependencies {
//        implementation(libs.jetbrains.coroutines.android)
//        implementation(libs.androidx.lifecycle.livedata)
//    }
//    sourceSets.jvmMain.dependencies {
//        implementation(libs.jetbrains.coroutines.jvm)
//    }
//
//    // Test Libraries
//    sourceSets.commonTest.dependencies {
//        implementation(libs.jetbrains.coroutines.test)
//        implementation(libs.jetbrains.kotlin.test)
//    }
//    sourceSets.javaTest.dependencies {
//        implementation(libs.mockk.test.agent)
//    }
//    sourceSets.androidUnitTest.dependencies {
//        implementation(libs.mockk.test.agent)
//        implementation(libs.mockk.test.android)
//        implementation(libs.androidx.test.core)
//    }
}
