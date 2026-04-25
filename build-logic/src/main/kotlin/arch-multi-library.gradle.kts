@file:Suppress("UnstableApiUsage", "OPT_IN_USAGE")

import com.android.build.api.variant.impl.capitalizeFirstChar
import com.android.build.api.withAndroid
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.kotlin.multiplatform.library")
}

private val formatName = project.name.split("-").joinToString("") { it }
    .replaceFirstChar { it.lowercase() }
extensions.configure<KotlinMultiplatformExtension> {
    compilerOptions {
        jvmToolchain(projectJavaVersionCode)
        progressiveMode.set(true)
    }
    withSourcesJar(true)
    applyDefaultHierarchyTemplate {
        common {
            group("java") {
                withJvm()
                withAndroid()
            }
            group("kotlin") {
                withJs()
                withWasmJs()
            }
        }
    }

    android {
        namespace = "br.com.arch.toolkit.${formatName}"
        testNamespace = "test.$namespace"
        androidResources { enable = false }
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

            htmlOutput = File("$rootDir/build/reports/lint/html/${formatName}-lint.html")
            xmlOutput = File("$rootDir/build/reports/lint/xml/${formatName}-lint.xml")
        }
        testCoverage { jacocoVersion = libraries.version("jacoco") }
        optimization.consumerKeepRules.file("consumer-proguard-rules.pro")
    }
    jvm { }
    wasmJs {
        browser { testTask { enabled = false } }
        binaries.library()
    }
    js(IR) {
        browser { testTask { enabled = false } }
        binaries.library()
    }
    // iOS Targets
    val exportName = project.name.split("-").joinToString(
        separator = "",
        transform = String::capitalizeFirstChar,
    )
    val exportId = "br.com.arch.toolkit.${formatName}"
    listOf(
        iosArm64(),
        iosX64(),
        iosSimulatorArm64(),
    ).forEach { target ->
        target.binaries.framework {
            baseName = "${exportName}Kit"
            isStatic = true
            freeCompilerArgs += listOf("-bundle-id", exportId)
        }
    }
}
