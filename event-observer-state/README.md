# 📦 Arch Toolkit · State Handle

[![Maven Central][badge-maven]][link-maven]
[![CI Status][badge-ci]][link-ci]
![Android][badge-android]
![Apple][badge-apple]
![JVM][badge-jvm]
![JS][badge-js]
![WASM][badge-wasm]
[![LICENSE][badge-license]][link-license]
[![COVERAGE][badge-coverage]][link-coverage]

A **typed, reactive, restorable UI state mechanism** built directly on top of
**Android’s `SavedStateHandle`** — used consistently across **multiplatform** targets.

This module is part of the
**[Arch Toolkit](https://github.com/matheus-corregiari/arch-toolkit).**

---

## ✨ Features

* ✅ **Single state model**: `SavedStateHandle` is the only storage source.
* 🔄 **Restart-safe**: Persists across config + process recreation (Android).
* 🌀 **Reactive**: Every value can be observed via `Flow`.
* ✍️ **Delegates**: `var value by state.value<T>()`
* 🧰 **ViewModel wrappers**: `saveState<T>()` and `saveResponseState<T>()`
* 🧩 **KMP support**: On non-Android targets, state is backed by `savedStateHandleCompat()`.
* 💾 **JSON fallback**: If a type can't be stored directly, it is stored as a JSON shadow
  automatically.

---

## 🚀 Quick Start

```kotlin
implementation("io.github.matheus-corregiari:state-handle:<version>")
```

---

## 📖 Usage (Android ViewModel)

```kotlin
class MyVm(
    private val state: SavedStateHandle
) : ViewModel() {

    // Optional state (null removes value)
    var query by state.value<String?>(key = "query")

    // Result-based state
    val profile by state.saveResponseState<User>()

    fun refresh(repo: Repo) = profile.load {
        repo.fetchUser() // -> Flow<DataResult<User>>
    }
}
```

React in UI:

```kotlin
val queryFlow = state.value<String?>(key = "query").flow()
val query by queryFlow.collectAsState(initial = null)
```

---

## 🔧 Property Delegates

| Delegate                | Behavior                             |
|-------------------------|--------------------------------------|
| `value<T?>()`           | Nullable, removes key on `null`.     |
| `.required { default }` | Non-null: always returns a fallback. |
| `.default { value }`    | Defaults on **read**, not persisted. |

```kotlin
var page by state.value<Int?>(key = "page").required { 1 }
```

---

## 🧱 ViewModel State Wrappers

### `Regular<T>`

```kotlin
val user by state.saveState<User>()
val flow: Flow<User?> = user.flow()
```

### `Result<T>`

```kotlin
val profile by state.saveResponseState<User>()

profile.load { repo.fetchUser() }
profile.flow() // Flow<DataResult<User>>
```

---

## 🌐 Multiplatform Behavior

| Platform           | How state is obtained                        | Notes                          |
|--------------------|----------------------------------------------|--------------------------------|
| **Android**        | Real `SavedStateHandle` (via `viewModel {}`) | Preferred                      |
| iOS, JVM, JS, WASM | `savedStateHandleCompat(name)`               | Backed by scoped in-memory map |

Enable compat once:

```kotlin
module {
    enableSavedStateHandleCompat()
}
```

Then:

```kotlin
class Presenter(scope: Scope) {
    private val state = scope.savedStateHandleCompat("screen-key")
    var filter by state.value<String?>("filter")
}
```

---

## 🧠 JSON Fallback

If a value cannot be written directly, it is stored as JSON:

| Key              | Meaning                                 |
|------------------|-----------------------------------------|
| `name`           | Stored when native persistence succeeds |
| `name-primitive` | JSON shadow fallback                    |

On read:
**native → else → JSON decode**

Keep state **small**: store **IDs**, not blobs.

---

## 🧪 Testing

In tests / preview environments:

```kotlin
val scope = GlobalScope // or custom test scope
val state = SavedStateHandle()
var counter by state.value<Int?>(key = "counter").required { 0 }

counter++
```

No mocks needed.

---

## 🧩 Related Modules

| Module              | Purpose                             |
|---------------------|-------------------------------------|
| `state-handle`      | This UI state layer                 |
| [storage-core](https://github.com/matheus-corregiari/arch-storage)      | Key–value persistence abstraction   |
| [storage-memory](https://github.com/matheus-corregiari/arch-storage)    | In-memory store for tests           |
| [storage-datastore](https://github.com/matheus-corregiari/arch-storage) | Persistent DataStore-backed storage |

---

## 👷 Part of Arch Toolkit

Designed for:

* Predictable UI state
* Total Kotlin-first ergonomics
* Realistic KMP architecture — no over-engineering

---

## 📄 License

This module is released under the **Apache 2.0 License**.
See [LICENSE](../../../LICENSE.md).

---

[link-maven]: https://search.maven.org/artifact/io.github.matheus-corregiari/state-handle

[link-ci]: https://github.com/matheus-corregiari/arch-toolkit/actions/workflows/ci.yml

[link-license]: ../../../LICENSE.md

[link-coverage]: https://codecov.io/gh/matheus-corregiari/arch-toolkit

[badge-android]: http://img.shields.io/badge/-android-6EDB8D.svg?style=flat

[badge-apple]: http://img.shields.io/badge/-apple-000000.svg?style=flat

[badge-js]: http://img.shields.io/badge/-js-F7DF1E.svg?style=flat

[badge-wasm]: http://img.shields.io/badge/-wasm-654FF0.svg?style=flat

[badge-jvm]: http://img.shields.io/badge/-jvm-DB413D.svg?style=flat

[badge-maven]: https://img.shields.io/maven-central/v/io.github.matheus-corregiari/state-handle.svg

[badge-ci]: https://github.com/matheus-corregiari/arch-toolkit/actions/workflows/ci.yml/badge.svg

[badge-license]: https://img.shields.io/github/license/matheus-corregiari/arch-toolkit

[badge-coverage]: https://img.shields.io/codecov/c/github/matheus-corregiari/arch-toolkit

---