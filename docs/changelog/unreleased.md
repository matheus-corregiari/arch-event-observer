# Unreleased

## Build and tooling

- Aligned Kotlin, Android Gradle Plugin, Detekt, Kover, and Gradle wrapper with the current Arch
  tooling baseline.
- Kept Compose Multiplatform `1.10.3` because the project still supports `iosX64`.
- Removed Dependabot in favor of the repository release-flow automation.
- CI now validates release and hotfix branches consistently across build, lint, test, coverage, and
  documentation workflows.

## Documentation

- Documented the supported Kotlin, Gradle, JDK, Android SDK, and Compose versions.
