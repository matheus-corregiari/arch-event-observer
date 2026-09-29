# Dependencies

Audited against Maven Central, Google Maven and the Gradle Plugin Portal on 2026-09-29 for `2.3.1`.
Runtime dependencies and AGP use stable releases. Detekt retains its existing alpha line.
Gradle **9.8.0**, JDK **21**, Kover **0.9.10**, MkDocs Material **9.7.7**.

| Alias                                      | Version         | Source                                                                                                                                                                 |
| ------------------------------------------ | --------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `jetbrains-dokka`                          | `2.2.0`         | [Metadata](https://repo.maven.apache.org/maven2/org/jetbrains/dokka/dokka-gradle-plugin/maven-metadata.xml)                                                            |
| `jetbrains-plugin`                         | `2.4.20`        | [Metadata](https://repo.maven.apache.org/maven2/org/jetbrains/kotlin/kotlin-gradle-plugin/maven-metadata.xml)                                                          |
| `jetbrains-multiplatform`                  | `2.4.20`        | [Metadata](https://repo.maven.apache.org/maven2/org/jetbrains/kotlin/multiplatform/org.jetbrains.kotlin.multiplatform.gradle.plugin/maven-metadata.xml)                |
| `jetbrains-kover`                          | `0.9.10`        | [Metadata](https://repo.maven.apache.org/maven2/org/jetbrains/kotlinx/kover-gradle-plugin/maven-metadata.xml)                                                          |
| `jetbrains-compose-runtime`                | `1.12.1`        | [Metadata](https://repo.maven.apache.org/maven2/org/jetbrains/compose/runtime/runtime/maven-metadata.xml)                                                              |
| `jetbrains-compose-animation`              | `1.12.1`        | [Metadata](https://repo.maven.apache.org/maven2/org/jetbrains/compose/animation/animation/maven-metadata.xml)                                                          |
| `jetbrains-compose-foundation`             | `1.12.1`        | [Metadata](https://repo.maven.apache.org/maven2/org/jetbrains/compose/foundation/foundation/maven-metadata.xml)                                                        |
| `jetbrains-compose-ui-test`                | `1.12.1`        | [Metadata](https://repo.maven.apache.org/maven2/org/jetbrains/compose/ui/ui-test/maven-metadata.xml)                                                                   |
| `jetbrains-compose-desktop`                | `1.12.1`        | [Metadata](https://repo.maven.apache.org/maven2/org/jetbrains/compose/desktop/desktop-jvm/maven-metadata.xml)                                                          |
| `jetbrains-compose-ui-test-junit4-desktop` | `1.12.1`        | [Metadata](https://repo.maven.apache.org/maven2/org/jetbrains/compose/ui/ui-test-junit4-desktop/maven-metadata.xml)                                                    |
| `jetbrains-coroutines-core`                | `1.11.0`        | [Metadata](https://repo.maven.apache.org/maven2/org/jetbrains/kotlinx/kotlinx-coroutines-core/maven-metadata.xml)                                                      |
| `jetbrains-coroutines-test`                | `1.11.0`        | [Metadata](https://repo.maven.apache.org/maven2/org/jetbrains/kotlinx/kotlinx-coroutines-test/maven-metadata.xml)                                                      |
| `jetbrains-kotlin-test`                    | `2.4.20`        | [Metadata](https://repo.maven.apache.org/maven2/org/jetbrains/kotlin/kotlin-test/maven-metadata.xml)                                                                   |
| `jetbrains-serialization-json`             | `1.11.0`        | [Metadata](https://repo.maven.apache.org/maven2/org/jetbrains/kotlinx/kotlinx-serialization-json/maven-metadata.xml)                                                   |
| `androidx-library`                         | `9.4.1`         | [Metadata](https://dl.google.com/dl/android/maven2/com/android/kotlin/multiplatform/library/com.android.kotlin.multiplatform.library.gradle.plugin/maven-metadata.xml) |
| `androidx-arch-coreTesting`                | `2.2.0`         | [Metadata](https://dl.google.com/dl/android/maven2/androidx/arch/core/core-testing/maven-metadata.xml)                                                                 |
| `androidx-compose-lifecycle`               | `2.11.0`        | [Metadata](https://dl.google.com/dl/android/maven2/androidx/lifecycle/lifecycle-runtime-compose/maven-metadata.xml)                                                    |
| `androidx-lifecycle-livedata`              | `2.11.0`        | [Metadata](https://dl.google.com/dl/android/maven2/androidx/lifecycle/lifecycle-livedata/maven-metadata.xml)                                                           |
| `androidx-lifecycle-runtime`               | `2.11.0`        | [Metadata](https://dl.google.com/dl/android/maven2/androidx/lifecycle/lifecycle-runtime/maven-metadata.xml)                                                            |
| `androidx-lifecycle-savedstate`            | `2.11.0`        | [Metadata](https://dl.google.com/dl/android/maven2/androidx/lifecycle/lifecycle-viewmodel-savedstate/maven-metadata.xml)                                               |
| `androidx-compose-testManifest`            | `1.12.1`        | [Metadata](https://dl.google.com/dl/android/maven2/androidx/compose/ui/ui-test-manifest/maven-metadata.xml)                                                            |
| `androidx-test-junit`                      | `1.3.0`         | [Metadata](https://dl.google.com/dl/android/maven2/androidx/test/ext/junit-ktx/maven-metadata.xml)                                                                     |
| `detekt`                                   | `2.0.0-alpha.6` | [Metadata](https://repo.maven.apache.org/maven2/dev/detekt/detekt-gradle-plugin/maven-metadata.xml)                                                                    |
| `ktlint`                                   | `14.2.0`        | [Metadata](https://plugins.gradle.org/m2/org/jlleitschuh/gradle/ktlint/org.jlleitschuh.gradle.ktlint.gradle.plugin/maven-metadata.xml)                                 |
| `vanniktech-publish`                       | `0.37.0`        | [Metadata](https://repo.maven.apache.org/maven2/com/vanniktech/gradle-maven-publish-plugin/maven-metadata.xml)                                                         |
| `robolectric-test`                         | `4.17`          | [Metadata](https://repo.maven.apache.org/maven2/org/robolectric/robolectric/maven-metadata.xml)                                                                        |
| `mockk-test-android`                       | `1.14.11`       | [Metadata](https://repo.maven.apache.org/maven2/io/mockk/mockk-android/maven-metadata.xml)                                                                             |
| `mockk-test-agent`                         | `1.14.11`       | [Metadata](https://repo.maven.apache.org/maven2/io/mockk/mockk/maven-metadata.xml)                                                                                     |

## Tooling sources

- [Gradle current release](https://services.gradle.org/versions/current)
- [MkDocs Material](https://pypi.org/project/mkdocs-material/)
- [JaCoCo](https://repo.maven.apache.org/maven2/org/jacoco/org.jacoco.core/maven-metadata.xml)

Robolectric 4.17 Android tests require `--add-opens=java.base/jdk.internal.access=ALL-UNNAMED`
on JDK 21. This option is scoped to test JVMs, following the
[Robolectric setup guide](https://robolectric.org/getting-started/).

Android SDK setup uses [`android-actions/setup-android@v4`](https://github.com/android-actions/setup-android/tree/v4)
with Node 24 and the maintained command-line tools provided by the action.

## JavaScript lockfile

Commit `kotlin-js-store/yarn.lock` to keep resolved npm dependencies reproducible in
local builds and CI, as recommended by the [Kotlin/JS setup guide](https://kotlinlang.org/docs/js-project-setup.html).
After an intentional dependency change, run `./gradlew kotlinUpgradeYarnLock` and
review the lockfile diff. Do not delete it to bypass dependency mismatches.

AtomicFU remains available for modules that use atomic state; the Compose module no
longer applies its plugin because observer registration is local to composition.

The CodeQL-only Kotlin 2.3.21 compiler enables `-Xexplicit-backing-fields` for the
Compose module. Normal builds use Kotlin 2.4.20, where this syntax is stable.

## Android SDK

Compile against stable Android SDK **37.2** with the `compileSdk { version = release(37) {
minorApiLevel = 2 } }` DSL. The catalog stores major and minor levels separately.
AGP **9.4.1** and Build Tools **37.0.0** are the latest stable releases verified in
[Google Maven](https://dl.google.com/dl/android/maven2/com/android/tools/build/gradle/maven-metadata.xml)
and the [SDK repository](https://dl.google.com/android/repository/repository2-3.xml).
`minSdk` remains **23**. These are KMP library modules; the consuming application owns
`targetSdk` and its runtime behavior. See the [Android SDK configuration guide](https://developer.android.com/build).
