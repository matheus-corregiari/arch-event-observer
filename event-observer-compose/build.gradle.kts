plugins {
    id("arch-multi-library")
    id("arch-lint")
    id("arch-documentation")
    id("arch-coverage")
    id("arch-optimize")
    id("arch-publish")
    alias(libs.plugins.jetbrains.compose.compiler)
    alias(libs.plugins.jetbrains.compose.kotlin)
}

kotlin {

    sourceSets {
        // Libraries
        commonMain.dependencies {
            implementation(project(":event-observer"))

            implementation(libs.jetbrains.coroutines.core)
            implementation(libs.androidx.compose.lifecycle)
            implementation(libs.jetbrains.compose.runtime)
            implementation(libs.jetbrains.compose.animation)
        }
        androidMain.dependencies {
            implementation(libs.androidx.lifecycle.livedata)
        }

        // Test Libraries
        commonTest.dependencies {
            implementation(project(":test"))
            implementation(libs.jetbrains.kotlin.test)
            implementation(libs.jetbrains.coroutines.test)
            implementation(libs.jetbrains.compose.foundation)
            implementation(libs.jetbrains.compose.ui.test)
        }
        jvmTest.dependencies {
            implementation(libs.jetbrains.compose.desktop)
            implementation(compose.desktop.currentOs)
            implementation(libs.jetbrains.compose.ui.test.junit4.desktop)
        }

        val nonJsTest = create("nonJsTest") {
            dependsOn(commonTest.get())
        }
        jvmTest.get().dependsOn(nonJsTest)
        wasmJsTest.get().dependsOn(nonJsTest)
        findByName("androidHostTest")?.dependsOn(nonJsTest)
        findByName("javaTest")?.dependsOn(nonJsTest)
        findByName("nativeTest")?.dependsOn(nonJsTest)
        findByName("iosArm64Test")?.dependsOn(nonJsTest)
        findByName("iosSimulatorArm64Test")?.dependsOn(nonJsTest)
    }
}
