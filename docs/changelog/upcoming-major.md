# Upcoming major: state modernization

Unreleased work based on `hotfix/2.3.1`. This is not a published release and must not be
shipped as a hotfix. The release number will be assigned during major-release preparation.

## Changes

- Publish payload and transient result status from one immutable snapshot; save prepared JSON
  before notifying collectors, including when collectors synchronously replace or clear state.
- Keep async producers, transforms, codecs and payload comparisons on the worker dispatcher;
  commit on the owner and reject obsolete operations. Recompare if the owner changes meanwhile.
- Retain API signatures, saved keys, JSON configuration, serializers, explicit nulls and defaults
  for absent entries. Preserve the last payload during loading/errors and after completion.
- Make SavedStateHandle keys holder-owned: raw writes no longer update active holders.
  Use `set` for small values or `setAsync` for expensive values. See [migration](../migration-state.md).
- Share expensive projections with native coroutine operators and worker-side output equality.
- Add permanent reentrancy, cancellation, snapshot consistency and platform restoration tests.
- Add opt-in Android app/Macrobenchmark modules with a frozen baseline and 100, 1,000 and
  10,000-record fixtures. Ordinary library publication and coverage exclude these modules.

## Validation and release gates

Local verification completed on 2026-10-01:

- JVM: 51 tests; Android host: 49; JS browser: 46; Wasm browser: 46.
- State coverage: 97.49% lines, 97.56% instructions and 83.87% branches; configured gates passed.
- Library build, state Detekt/Ktlint/Android lint, generated API docs and strict MkDocs passed.
- Six Android emulator benchmark cases passed, one iteration each: old/new implementations at
  all fixture sizes, including rotation and restoration after background-process death.

Before release, execute iOS tests on macOS and compare release-build frame metrics on a physical
Android device against measured variability. Emulator results validate the harness, not frame
acceptance. See [performance measurements and limits](../state-performance.md).

The unused Compose import was removed during the 2026-10-01 CI readiness review.
