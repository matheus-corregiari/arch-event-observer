# Unreleased

## Build and tooling

- Aligned Kotlin, Android Gradle Plugin, Detekt, Kover, and Gradle wrapper with the current Arch
  tooling baseline.
- Updated Compose Multiplatform to `1.12.0`, aligned Kotlin to the compatible `2.3.21` line, removed obsolete `iosX64`, and migrated Compose tests to v2 with test-only JS/Wasm executables.
- Removed Dependabot in favor of the repository release-flow automation.
- CI now validates release and hotfix branches consistently across build, lint, test, coverage, and
  documentation workflows.

## Documentation

- Documented the supported Kotlin, Gradle, JDK, Android SDK, and Compose versions.
