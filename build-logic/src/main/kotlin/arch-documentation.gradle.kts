/**
 * Configures documentation and coverage tooling for publishable modules.
 *
 * The plugin enables Dokka, Kover, and Jacoco with repository-wide defaults so CI can generate API
 * reference material and coverage reports consistently.
 */
import org.gradle.internal.extensions.stdlib.capitalized
import org.jetbrains.dokka.gradle.DokkaExtension

plugins {
    id("org.jetbrains.dokka")
    id("org.jetbrains.kotlinx.kover")
    jacoco
}
extensions.configure(JacocoPluginExtension::class) {
    toolVersion = libraries.version("jacoco")
}

extensions.configure(DokkaExtension::class) {
    moduleName.set(project.name.capitalized())
    moduleVersion.set(project.versionName)
    basePublicationsDirectory.set(file("$rootDir/docs/api/${project.name}"))
    dokkaPublications.getByName("html").enabled.set(false)

    dokkaSourceSets.configureEach {
        reportUndocumented.set(true)
        skipDeprecated.set(true)
        skipEmptyPackages.set(true)
        jdkVersion.set(projectJavaVersionCode)
        enableAndroidDocumentationLink.set(true)
        enableJdkDocumentationLink.set(true)
        enableKotlinStdLibDocumentationLink.set(true)
    }
}
