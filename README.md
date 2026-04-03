# Arch Android Toolkit

A Kotlin-first Android toolkit with APIs designed to fit well in **KMP projects** (Android source set)
while keeping day-to-day Android development ergonomic.

[![Maven Central][badge-maven]][link-maven]
[![License][badge-license]](/LICENSE)
[![Kotlin][badge-kotlin]](https://kotlinlang.org)
![Lint][badge-lint]
![Test][badge-test]
[![Coverage][badge-coverage]][link-coverage]

---

## ✨ Features

- **State machines** for view and scene orchestration (`StateMachine`, `ViewStateMachine`, `SceneStateMachine`).
- **Storage abstractions** with in-memory and SharedPreferences implementations.
- **Delegates for persisted properties** to reduce boilerplate for cached/config values.
- **Recycler adapter utilities** including generic binders and sticky-header support.
- **Foldable helpers** to react to hinge/posture changes.
- **Application context provider** with `ContextProvider`.

## 📦 Installation

```kotlin
// build.gradle.kts

// If your project is KMP
kotlin {
    sourceSets {
        androidMain {
            dependencies {
                implementation("io.github.matheus-corregiari:arch-android:<latest-version>")
            }
        }
    }
}

// If your project is only Android
dependencies {
    implementation("io.github.matheus-corregiari:arch-android:<latest-version>")
}
```

## 🛠️ Usage

### 1) Configure storage at startup

```kotlin
class App : Application() {
    override fun onCreate() {
        super.onCreate()

        Storage.KeyValue.init(this)
        ContextProvider.init(this)
    }
}
```

### 2) Build a simple state machine

```kotlin
val machine = ViewStateMachine()
machine.setup {
    state(0) { visibles(viewA); gones(viewB) }
    state(1) { visibles(viewB); gones(viewA) }
}

machine.changeState(0)
```

### 3) Persist a typed config value

```kotlin
val darkMode = ConfigValue(
    name = "dark_mode",
    default = false,
    storage = { Storage.KeyValue.regular }
)

darkMode.set(true)
val enabled = darkMode.get()
```

## 🌍 Platform Support

| Target      | Support |
|:------------|:--------|
| **Android** | ✅      |

> This module is Android-focused, but the public API style favors KMP-friendly usage from
> `androidMain` and shared architecture layers.

## 🏗️ Built With

| Tool       | Version  |
|:-----------|:---------|
| **Kotlin** | `2.3.10` |
| **Gradle** | `9.3.1`  |
| **Java**   | `21`     |

## 🔍 Quality Notes

- KDocs are written in English and optimized for Dokka rendering.
- `ContextProvider` uses `WeakReference` to reduce Activity leak risk.
- `ObservableValue` maintains an internal coroutine scope; prefer lifecycle-bounded instances.

## 🤝 Contributing

Contributions are welcome! If you find a bug or have a feature request, please open an issue. If
you'd like to contribute code, please fork the repository and submit a pull request.

Please read [CONTRIBUTING](CONTRIBUTING.md) for a straightforward, KMP-focused workflow.

## 📚 Documentation

For detailed API information, please refer to the [KDocs](/docs/api/android/index.md).

## 📄 License

```text
Copyright 2025 Matheus Corregiari

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

[link-maven]: https://search.maven.org/artifact/io.github.matheus-corregiari/arch-android
[link-coverage]: https://codecov.io/gh/matheus-corregiari/arch-android

[badge-kotlin]: https://img.shields.io/badge/kotlin-2.3.10-blue.svg?logo=kotlin
[badge-maven]: https://img.shields.io/maven-central/v/io.github.matheus-corregiari/arch-android.svg
[badge-license]: https://img.shields.io/github/license/matheus-corregiari/arch-android
[badge-coverage]: https://codecov.io/gh/matheus-corregiari/arch-android/graph/badge.svg?token=146UU167K6
[badge-lint]: https://github.com/matheus-corregiari/arch-android/actions/workflows/lint.yml/badge.svg
[badge-test]: https://github.com/matheus-corregiari/arch-android/actions/workflows/coverage.yml/badge.svg
