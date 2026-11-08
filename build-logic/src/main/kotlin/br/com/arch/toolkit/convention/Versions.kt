package br.com.arch.toolkit.convention

import org.gradle.api.Project
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.gradle.process.ExecOperations
import java.io.ByteArrayOutputStream
import javax.inject.Inject

object Versions {
    const val JAVA: Int = 25

    const val MIN_SDK: Int = 23
    const val COMPILE_SDK: Int = 37
    const val COMPILE_SDK_MINOR: Int = 2
    const val BUILD_TOOLS: String = "37.0.0"

    const val JACOCO: String = "0.8.16"

    fun getVersionName(project: Project): String {
        val releaseVersion = project.providers.gradleProperty("releaseVersion").orNull
        if (releaseVersion != null) {
            require(releaseVersion.isNotBlank()) { "releaseVersion must not be blank" }
            return releaseVersion
        }

        val fileVersion = project.rootProject.layout.projectDirectory
            .file("build/version-name.txt")
            .asFile
            .takeIf { it.exists() }
            ?.readText()
            ?.trim()
            ?.takeIf { it.isNotBlank() }

        if (fileVersion != null) {
            return fileVersion
        }

        val gitVersion = project.providers.of(GitDescribeSource::class.java) {}.get()
        return gitVersion.ifBlank { "0.0.0" }
    }
}

val Project.versionName: String
    get() = Versions.getVersionName(this)

abstract class GitDescribeSource : ValueSource<String, ValueSourceParameters.None> {
    @get:Inject
    abstract val execOperations: ExecOperations

    override fun obtain(): String = try {
        val out = ByteArrayOutputStream()
        val result = execOperations.exec {
            commandLine("git", "describe")
            standardOutput = out
            isIgnoreExitValue = true
        }
        if (result.exitValue == 0) out.toString().trim() else ""
    } catch (@Suppress("TooGenericExceptionCaught", "SwallowedException") ignored: Exception) {
        ""
    }
}
