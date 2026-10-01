# Dependencies

Audited against Maven Central, Google Maven and the Gradle Plugin Portal on 2026-10-01 for `2.3.1`.
Runtime dependencies and AGP use stable releases. Detekt retains its existing alpha line.
Gradle **9.8.0**, JDK **21**, Kover **0.9.11**, MkDocs Material **9.7.7**.

| Alias                                      | Version         | Source                                                                                                                                                                 |
| ------------------------------------------ | --------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `jetbrains-dokka`                          | `2.2.0`         | [Metadata](https://repo.maven.apache.org/maven2/org/jetbrains/dokka/dokka-gradle-plugin/maven-metadata.xml)                                                            |
| `jetbrains-plugin`                         | `2.4.20`        | [Metadata](https://repo.maven.apache.org/maven2/org/jetbrains/kotlin/kotlin-gradle-plugin/maven-metadata.xml)                                                          |
| `jetbrains-multiplatform`                  | `2.4.20`        | [Metadata](https://repo.maven.apache.org/maven2/org/jetbrains/kotlin/multiplatform/org.jetbrains.kotlin.multiplatform.gradle.plugin/maven-metadata.xml)                |
| `jetbrains-kover`                          | `0.9.11`        | [Metadata](https://repo.maven.apache.org/maven2/org/jetbrains/kotlinx/kover-gradle-plugin/maven-metadata.xml)                                                          |
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

CodeQL and normal builds both use Kotlin 2.4.20, where explicit backing fields are
stable. [CodeQL 2.27.1 supports Kotlin 2.4.20](https://github.blog/changelog/2026-09-25-codeql-2-27-1-adds-c-and-c-query-and-kotlin-2-4-20-support/),
so no compiler downgrade or backing-field opt-in is needed.

## Android SDK

Compile against stable Android SDK **37.2** with the `compileSdk { version = release(37) {
minorApiLevel = 2 } }` DSL. The catalog stores major and minor levels separately.
AGP **9.4.1** and Build Tools **37.0.0** are the latest stable releases verified in
[Google Maven](https://dl.google.com/dl/android/maven2/com/android/tools/build/gradle/maven-metadata.xml)
and the [SDK repository](https://dl.google.com/android/repository/repository2-3.xml).
`minSdk` remains **23**. These are KMP library modules; the consuming application owns
`targetSdk` and its runtime behavior. See the [Android SDK configuration guide](https://developer.android.com/build).

## Stable update assessment (2026-10-01)

- Adopt Kover **0.9.11**. Its [release](https://github.com/Kotlin/kotlinx-kover/releases/tag/v0.9.11)
  reverts a 0.9.10 change that produced zero coverage with Gradle configuration cache
  ([issue 831](https://github.com/Kotlin/kotlinx-kover/issues/831)). Configuration cache remains disabled here;
  retain report filters and module thresholds and verify coverage before accepting the update.
- Gradle **9.8.0**, Kotlin **2.4.20**, Compose **1.12.1**, AGP **9.4.1**, SDK **37.2**,
  Build Tools **37.0.0**, AtomicFU **0.33.0**, Dokka **2.2.0**, coroutines/serialization **1.11.0**,
  JaCoCo **0.8.15**, ktlint **14.2.0**, publishing **0.37.0**, Robolectric **4.17** and MockK **1.14.11**
  match stable metadata. The dependency table covers every declared Maven library.
- Detekt **2.0.0-alpha.6** has no stable release in the `dev.detekt` line. Keep the existing tool;
  reverting to the old stable 1.x coordinates would be a migration, not a patch update.
- Retain current major references for Actions: checkout/Python/artifact/Codecov **v7**, Java/Gradle **v6**,
  Android/CodeQL **v4**, GitHub App/release **v3** and Pages **v5**. Latest stable tags remain in
  those majors; floating major tags receive their compatible patch updates.
- MkDocs Material **9.7.7** remains current. Library modules have no application `targetSdk`;
  opt-in benchmark apps use the catalog's API **37**. Compile SDK **37.2** is a minor API level,
  not a reason to invent a library target API or raise `minSdk` **23**.
- Do not introduce Compose 1.13 alpha or prerelease AGP/Kotlin in this publication.
