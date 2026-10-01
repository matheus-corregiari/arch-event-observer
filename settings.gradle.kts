@file:Suppress("UnstableApiUsage")

pluginManagement {
    apply(from = "$rootDir/gradle/repositories.gradle.kts")
    val repositoryList = extra["repositoryList"] as RepositoryHandler.() -> Unit
    repositories(repositoryList)
    includeBuild("build-logic")
}

dependencyResolutionManagement {
    apply(from = "$rootDir/gradle/repositories.gradle.kts")
    val repositoryList = extra["repositoryList"] as RepositoryHandler.() -> Unit
    repositories(repositoryList)
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
}

include(":event-observer")
include(":event-observer-compose")
include(":event-observer-state")
include(":test")

// Device benchmarks are opt-in and never participate in publication or ordinary coverage.
if (providers.gradleProperty("stateBenchmarks").orNull == "true") {
    include(":state-benchmark-app", ":state-macrobenchmark")
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

