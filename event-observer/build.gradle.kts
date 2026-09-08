plugins {
    id("arch-multi-library")
    id("arch-lint")
    id("arch-documentation")
    id("arch-optimize")
    id("arch-publish")
    alias(libs.plugins.jetbrains.atomic)
}

kotlin {
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
        commonTest.dependencies {
            implementation(libs.jetbrains.coroutines.test)
            implementation(libs.jetbrains.kotlin.test)
        }
        javaTest.dependencies {
            implementation(libs.jetbrains.coroutines.test)
            implementation(libs.jetbrains.kotlin.test)
            implementation(libs.mockk.test.agent)
        }
        androidHostTest.dependencies {
            implementation(libs.jetbrains.coroutines.test)
            implementation(libs.jetbrains.kotlin.test)
            implementation(libs.mockk.test.android)
            implementation(libs.androidx.arch.coreTesting)
        }
    }
}
