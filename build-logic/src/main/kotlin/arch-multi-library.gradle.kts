@file:Suppress("UnstableApiUsage", "OPT_IN_USAGE")
@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

/**
 * Configures a Kotlin Multiplatform Android library module.
 *
 * The plugin owns common compiler, hierarchy, Android namespace, lint, test coverage, native
 * framework, and source jar defaults shared by publishable library modules.
 */
import com.android.build.api.variant.impl.capitalizeFirstChar
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
                withAndroidTarget()
            }
            group("kotlin") {
                withJs()
                withWasmJs()
            }
        }
    }

    android {
        namespace = "br.com.arch.toolkit.$formatName"
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

        }
        testCoverage { jacocoVersion = libraries.version("jacoco") }
        project.file("consumer-proguard-rules.pro")
            .takeIf(File::exists)
            ?.let(optimization.consumerKeepRules::file)
    }
    jvm { }
    wasmJs {
        browser { testTask { useKarma { useChromeHeadless() } } }
        binaries.library()
    }
    js {
        browser { testTask { useKarma { useChromeHeadless() } } }
        binaries.library()
    }
    // iOS Targets
    val exportName = project.name.split("-").joinToString(
        separator = "",
        transform = String::capitalizeFirstChar
    )
    val exportId = "br.com.arch.toolkit.$formatName"
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { target ->
        target.binaries.framework {
            baseName = "${exportName}Kit"
            isStatic = true
            freeCompilerArgs += listOf("-bundle-id", exportId)
        }
    }
}

pluginManager.withPlugin("org.jetbrains.compose") {
    extensions.configure<KotlinMultiplatformExtension> {
        js {
            binaries.executable()
            binaries.executable(compilations["test"])
        }
        wasmJs {
            binaries.executable()
            binaries.executable(compilations["test"])
        }
    }

    tasks.named("jsBrowserProductionLibraryDistribution") {
        dependsOn("jsProductionExecutableCompileSync")
    }
    tasks.named("jsBrowserProductionWebpack") {
        dependsOn("jsProductionLibraryCompileSync")
    }
    tasks.named("wasmJsBrowserProductionLibraryDistribution") {
        dependsOn("wasmJsProductionExecutableCompileSync")
    }
    tasks.named("wasmJsBrowserProductionWebpack") {
        dependsOn("wasmJsProductionLibraryCompileSync")
    }
}
