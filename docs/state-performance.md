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

| Items | Asynchronous save | 100 subsequent UI reads |
| --- | --- | --- |
| 100,000 | 155 ms | 2.8 ms |
| 1,000,000 | 556 ms | 0.6 ms |

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
  operations/projections for expensive updates. External raw-key writes also decode on the next read.
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
