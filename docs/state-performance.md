# State performance and UI responsiveness

## What the tests establish

The regression suite uses lists of 100,000 and 1,000,000 items and a projection performing five million arithmetic
steps. Before the change, 100 reads decoded the large JSON value 100 times, and a busy transformation
prevented a queued UI callback from executing. Both regressions failed against the previous implementation.

The implementation now prepares JSON and a detached decoded snapshot on the worker dispatcher for
operations and `setAsync`. Ordinary reads reuse that snapshot. `selectAsync` computes away from the
owner scope and retains the last completed result. Producers commit sequentially with no extra buffer,
so a source error does not discard a preceding emitted value. Replacement still rejects stale work.

Two kinds of tests serve different purposes:

- `commonTest/LargeStateTest`: large-list decode counts, async writes and expensive projections,
  shared across targets. Deterministic assertions check work counts and results, not a machine-specific
  frame-time threshold. Browser stress tests use a finite 60-second Mocha timeout; the default
  two seconds is too short for the five-million-step JS workload.
- `jvmTest/UiResponsivenessTest`: a real single-thread UI executor and a separate worker. A latch holds
  a non-suspending transformation busy while a UI callback must execute. A second test verifies that
  serialization and decoding of 100,000 items never execute on that UI executor and do not repeat on reads.
  A third test holds an obsolete serializer busy and verifies that a replacement can finish without
  waiting for it, and that its stale result never overwrites the new state.
  Timings are printed into the test reports for diagnosis.

A local Windows/JDK 21 instrumented run produced:

| Items     | Asynchronous save | 100 subsequent UI reads |
| --------- | ----------------- | ----------------------- |
| 100,000   | 155 ms            | 2.8 ms                  |
| 1,000,000 | 556 ms            | 0.6 ms                  |

Encoding and decoding ran on workers, and the reads performed no additional codec calls. These are observations, not latency guarantees or release benchmarks.
The gating tests passed after the change. Garbage collection, device contention, rendering and the
platform's own saved-state lifecycle are outside these timings.

## API choice

```kotlin
// Inside a ViewModel: the producer and transformation execute on the worker dispatcher.
fun refresh() {
    val query = filter.get().orEmpty() // Capture saved state before switching contexts.
    users.loadMapped(transform = { dtos -> dtos.map { dto -> dto.toUiModel() } }) {
        repository.users(query)
    }
}

// Direct large update; await completion with the returned Job when necessary.
val write = users.setAsync(items)

// Expensive projection; reading visible.value does not rerun filtering/sorting.
val visible = users.selectAsync(initialValue = emptyList<User>()) { values ->
    values.orEmpty().filter { it.active }.sortedBy { it.name }
}
```

The owner scope must remain main-thread confined. Codecs, producers and transformations must be safe
for worker execution; capture UI inputs first, and keep submitted and returned snapshots immutable.
Custom serializers may be invoked by overlapping cancelled/replacement work. Use stateless serializers
or provide appropriate synchronization. CPU work that ignores cancellation may finish in the background,
but its obsolete result is not committed. Add suspension/cancellation checks in long loops to stop it sooner.

## Limits that must remain explicit

There is no universal promise that arbitrary code can never freeze the UI:

- `set`, delegated property assignments, construction/restoration and `select` are synchronous APIs.
  Initial restoration still decodes on the caller thread. Use compact saved models and asynchronous
  operations/projections for expensive updates. Raw writes to a privately owned key are unsupported.
- On JVM/Android and Native, `Dispatchers.Default` uses background threads. On browser JS/Wasm,
  changing dispatcher does not create a worker thread. A single huge synchronous serializer or mapper
  can still block the event loop. Split work with suspension points (for example `yield()` between
  batches), keep the saved payload compact, or offload a platform-specific operation to a Web Worker.
  A suspension between batches cannot preempt one expensive element or a synchronous JSON codec.
- SavedStateHandle commits and platform save/restore are synchronous. Persisting huge lists can exceed
  saved-state size limits regardless of dispatcher. The stress test is not a recommendation to save
  100,000 production records. Store IDs, filters, or a compact screen snapshot and reload the dataset
  from the Repository/cache.
- `selectAsync` starts eagerly in its supplied scope and propagates errors there. Supply an appropriate
  scope or handle expected transformation errors explicitly; it does not invent a fallback policy.

The tests demonstrate worker separation and avoidance of repeated computation. They do not certify
16 ms frame times on every device or browser.

References: [Default dispatcher](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/-dispatchers/-default.html),
[Main dispatcher](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/-dispatchers/-main.html),
and [cooperative yield](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/yield.html).

## Snapshot publication in 3.0.0

Holder flows project one immutable snapshot held by MutableStateFlow. Revision identities
make publication and built-in payload/result projections independent of deep payload equality.
Async preparation includes encoding, detached decoding and equality comparison on the worker.
`selectAsync` also publishes its projected value and compares outputs on the worker, under the
original scope Job; an expensive output equality check therefore does not migrate back to the UI.
If the owner changes while comparison runs, it is repeated on the worker before committing.
The handle receives prepared JSON before publication. There is no post-publication mutation.
Equal payloads reuse the previous immutable payload. User `select` functions and their output
equality run in their collector context and must remain cheap; `selectAsync` is the heavy-work API.

The platform still copies/marshals saved JSON synchronously. Pre-encoding does not eliminate
that cost or Android's saved-state size limit. Restore decodes once on the owner to preserve
immediate initial reads. Custom serializers and equality implementations must be safe for workers.

`PublicationPerformanceTest` measures 100, 1,000 and 10,000 integer records with live data/result
subscribers. It asserts codecs and payload equality stay off the owner and prints JSON byte size,
async latency, 100-read time and allocated bytes on the owner. JVM allocation counts come from
ThreadMXBean and include coroutine scheduling/measurement overhead. Large-list tests remain codec
stress tests, not recommendations for Android saved state.

## Android frame comparison

Opt in with `-PstateBenchmarks=true`; ordinary CI/publication excludes the benchmark modules.
The app includes a frozen baseline from hotfix commit `f483b58` and the new implementation in the
same APK, with identical UI, dependencies and integer fixtures. Run on a physical API 29+ device:

```shell
./gradlew -PstateBenchmarks=true :state-macrobenchmark:connectedReleaseAndroidTest
```

The parameterized benchmark measures both backends at 100, 1,000 and 10,000 rows, ten iterations
per case with full compilation. It renders a list, refreshes, merges, scrolls, rotates, backgrounds,
kills the background process and restores. FrameTimingMetric records frame costs; StateSave and
StateRestore trace metrics measure lifecycle costs. Logcat `StateBenchmark` records saved Bundle
bytes and process allocation deltas (including UI work). Keep JSON reports and Perfetto traces
from `build/bench/test/outputs/`. Benchmark build paths are shortened for Windows trace exports.

Compare frame CPU/overrun percentiles on the same device, refresh rate and power/thermal conditions.
Repeat full runs to establish variability; a regression outside that observed variability blocks
release. Keep device measurements separate from deterministic CI. Emulator runs only validate the
harness and cannot certify hardware frame performance. No measured Android frame envelope is
claimed until physical-device reports are available. iOS requires macOS for its test target.

## Android harness validation (2026-10-01)

All six cases passed on an API 37.1 Android emulator: both backends at 100, 1,000 and
10,000 rows, one iteration per case. Rotation and background-process death restored the
payload without a new load (`sequence=0`). The complete Activity Bundle, measured through
Parcel, contained 2,272, 9,476 and 99,484 bytes respectively for these integer fixtures.
This validates the harness and platform save/restore path, not hardware frame acceptance.

For a quick harness run, pass `stateBenchmarkIterations=1` as an instrumentation argument
and explicitly suppress the `EMULATOR` benchmark check. `stateBenchmarkSmoke=true` limits
that run to 100 rows. Do not suppress device checks for the physical-device acceptance run.

## Current local JVM observations

A Windows/JDK 21 instrumented run with live payload/result collectors produced:

| Integer records | JSON UTF-8 bytes | Async write | 100 owner reads | Owner allocated bytes |
| --------------- | ---------------- | ----------- | --------------- | --------------------- |
| 100             | 291              | 13.28 ms    | 3.67 ms         | 1,169,136             |
| 1,000           | 3,891            | 8.45 ms     | 0.33 ms         | 8,896                 |
| 10,000          | 48,891           | 15.12 ms    | 0.36 ms         | 53,888                |

The first fixture includes cold startup overhead. These are diagnostic observations from
`PublicationPerformanceTest`, not warmed benchmarks or Android frame acceptance results.
All recorded codec and payload-equality calls ran outside the owner thread. Allocations include
the test fixture's JSON byte-count conversion and coroutine scheduling; they are not exclusively
holder allocations. Do not infer a production limit from these integer-only fixtures.
