@file:Suppress("UnstableApiUsage", "OPT_IN_USAGE")

import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.kotlin.multiplatform.library")
}

private val formatName = project.name.split("-").joinToString("") { it }.replaceFirstChar { it.lowercase() }
extensions.configure<KotlinMultiplatformExtension> {
    compilerOptions { jvmToolchain(projectJavaVersionCode) }
    withSourcesJar(true)
    androidLibrary {
        namespace = "br.com.arch.toolkit.$formatName"
        testNamespace = "test.$namespace"
        androidResources { enable = true }
        withHostTest {
            enableCoverage = true
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
        lint {
            checkReleaseBuilds = true
            abortOnError = true
            ignoreWarnings = false
            absolutePaths = false
            warningsAsErrors = false

            htmlOutput = File("$rootDir/build/reports/lint/html/$formatName-lint.html")
            xmlOutput = File("$rootDir/build/reports/lint/xml/$formatName-lint.xml")
        }
        testCoverage { jacocoVersion = libraries.version("jacoco") }
        optimization.consumerKeepRules.file("consumer-proguard-rules.pro")
    }
}
