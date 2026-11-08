@file:Suppress("ktlint:standard:filename")

import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.findByType
import kotlin.jvm.optionals.getOrNull

internal val Project.libraries: VersionCatalog
    get() = extensions.findByType<VersionCatalogsExtension>()?.named("libs")
        ?: error("Cannot find libraries in version catalog!")

internal val VersionCatalog.allDefinedDependencies: Set<String>
    get() = libraryAliases.asSequence().map(::findLibrary).mapNotNull {
        it.getOrNull()?.get()?.run {
            "${module.group}:${module.name}:${versionConstraint.requiredVersion}:"
        }
    }.toSet()
