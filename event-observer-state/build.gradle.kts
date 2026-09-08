import dev.detekt.gradle.Detekt
import kotlinx.kover.gradle.plugin.dsl.CoverageUnit

plugins {
    id("arch-multi-library")
    id("arch-lint")
    id("arch-documentation")
    id("arch-optimize")
    id("arch-publish")

    alias(libs.plugins.jetbrains.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":event-observer"))
            api(libs.androidx.lifecycle.savedstate)
            api(libs.jetbrains.serialization.json)
            api(libs.jetbrains.coroutines.core)
        }
        commonTest.dependencies {
            implementation(libs.jetbrains.kotlin.test)
            implementation(libs.jetbrains.coroutines.test)
        }
        androidHostTest.dependencies {
            implementation(libs.robolectric.test)
        }
    }
}

// The generic detekt task does not discover KMP sources automatically.
tasks.named<Detekt>("detekt") {
    setSource(fileTree("src/commonMain") { include("**/*.kt") })
}

kover {
    reports {
        total {
            verify {
                rule("State line coverage") { minBound(90, CoverageUnit.LINE) }
                rule("State instruction coverage") { minBound(85, CoverageUnit.INSTRUCTION) }
                rule("State branch coverage") { minBound(80, CoverageUnit.BRANCH) }
            }
        }
    }
}
