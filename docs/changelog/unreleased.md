# Unreleased

## Build and tooling

- Aligned Kotlin, Android Gradle Plugin, Detekt, Kover, and Gradle wrapper with the current Arch
  tooling baseline.
- Kept Compose Multiplatform `1.10.3` because the current Kotlin `2.4.10` and `iosX64` matrix are not compatible with the newer Compose v2 test API.
- Removed Dependabot in favor of the repository release-flow automation.
- CI now validates release and hotfix branches consistently across build, lint, test, coverage, and
  documentation workflows.

## Documentation

- Documented the supported Kotlin, Gradle, JDK, Android SDK, and Compose versions.
