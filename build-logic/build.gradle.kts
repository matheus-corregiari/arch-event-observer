plugins {
    `kotlin-dsl`
    alias(libs.plugins.ktlint)
}

configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
    filter {
        exclude { it.file.startsWith(layout.buildDirectory.get().asFile) }
    }
}

kotlin { jvmToolchain(25) }

group = "com.toolkit.plugin"
version = "1.0.0"

dependencies {
    compileOnly(gradleApi())
    implementation(libs.androidx.library)
    implementation(libs.jetbrains.plugin)
    implementation(libs.jetbrains.multiplatform)
    implementation(libs.jetbrains.dokka)
    implementation(libs.jetbrains.kover)
    implementation(libs.vanniktech.publish)

    // Lint
    implementation(libs.detekt)
    implementation(libs.ktlint)
}

configurations.matching { !it.name.contains("ktlint", ignoreCase = true) }.configureEach {
    resolutionStrategy {
        failOnVersionConflict()
        preferProjectModules()
        enableDependencyVerification()
        eachDependency {
            when (target.group) {
                "org.jetbrains.kotlin" -> useVersion(embeddedKotlinVersion)
            }
        }
    }
}
