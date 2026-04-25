plugins {
    alias(libs.plugins.jetbrains.atomic) apply false
    alias(libs.plugins.jetbrains.compose.compiler) apply false
    alias(libs.plugins.jetbrains.kover)
    alias(libs.plugins.vanniktech.publish) apply false
}

dependencies {
    subprojects
        .filter { it.name != "test" }
        .forEach { add("kover", project(it.path)) }
}

val syncContributingDocs by tasks.registering(Copy::class) {
    description = "Syncs CONTRIBUTING.md into the MkDocs source tree."
    from(layout.projectDirectory.file("CONTRIBUTING.md"))
    into(layout.projectDirectory.dir("docs"))
    rename { "contributing.md" }
}

val ciLint by tasks.registering {
    group = "verification"
    description = "Runs lint checks for all modules that expose lint tasks."
}

val ciDocs by tasks.registering {
    group = "documentation"
    description = "Generates API documentation inputs for the MkDocs site."
    dependsOn(syncContributingDocs)
}

val ciBuild by tasks.registering {
    group = "build"
    description = "Assembles all publishable modules."
}

val ciTest by tasks.registering {
    group = "verification"
    description = "Runs all supported test tasks."
}

val ciCoverage by tasks.registering {
    group = "verification"
    description = "Runs tests and verifies merged coverage."
    dependsOn(ciTest, "koverXmlReport", "koverHtmlReport", "koverVerify")
}

val ciPublishMavenCentral by tasks.registering {
    group = "publishing"
    description = "Publishes all publishable modules to Maven Central."
}

val ciPublishGithubPackages by tasks.registering {
    group = "publishing"
    description = "Publishes all publishable modules to GitHub Packages."
}

val ciPublishLocal by tasks.registering {
    group = "publishing"
    description = "Publishes all publishable modules to the local Maven repository."
}

gradle.projectsEvaluated {
    val multiplatformProjects = subprojects.filter {
        it.plugins.hasPlugin("org.jetbrains.kotlin.multiplatform")
    }
    val publishableProjects = subprojects.filter {
        it.plugins.hasPlugin("com.vanniktech.maven.publish")
    }

    fun Project.taskPath(name: String): String? = tasks.findByName(name)?.path

    ciLint.configure {
        dependsOn(multiplatformProjects.mapNotNull { it.taskPath("detekt") })
        dependsOn(multiplatformProjects.mapNotNull { it.taskPath("ktlintCheck") })
    }
    ciDocs.configure {
        dependsOn(publishableProjects.mapNotNull { it.taskPath("dokkaGeneratePublicationHtml") })
    }
    ciBuild.configure {
        dependsOn(publishableProjects.mapNotNull { it.taskPath("assemble") })
    }
    ciTest.configure {
        dependsOn(multiplatformProjects.mapNotNull { it.taskPath("allTests") })
    }
    ciPublishMavenCentral.configure {
        dependsOn(publishableProjects.mapNotNull { it.taskPath("publishAndReleaseToMavenCentral") })
    }
    ciPublishGithubPackages.configure {
        dependsOn(publishableProjects.mapNotNull { it.taskPath("publishAllPublicationsToGithubRepository") })
    }
    ciPublishLocal.configure {
        dependsOn(publishableProjects.mapNotNull { it.taskPath("publishToMavenLocal") })
    }
}
