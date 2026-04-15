# Contributing to Arch Event Observer

Thanks for taking the time to contribute.

## Quick Start

1. Fork and clone the repo.
2. Use JDK `21`.
3. Build once to warm up Gradle:

```bash
./gradlew build
```

## Repository Layout

- `event-observer/` -> shared result, flow, and LiveData APIs
- `event-observer-compose/` -> Compose-facing observation APIs
- `docs/` -> published MkDocs content

## Development Workflow

1. Create a branch from `master`.
2. Keep changes small and scoped.
3. Keep public docs, README, and KDoc aligned with shipped behavior.
4. Add or update tests when behavior changes.
5. Avoid unrelated refactors.

## Local Validation

```bash
./gradlew build
./gradlew allTests
./gradlew detekt
./gradlew ktlintCheck
```

Use the project wrapper and toolchain settings when validating changes.

## Documentation Expectations

- Keep examples short, real, and tied to the public API.
- Explain status handling with `DataResultStatus`, `EventDataStatus`, and `DataResult<T>` when relevant.
- Mention platform differences when they affect behavior.
- Update README, public docs under `docs/`, and release notes when public behavior changes.

## Dependency Hygiene

- Keep direct build and tooling dependencies current when updates are low risk.
- Prefer stable releases over RC, beta, or alpha unless a prerelease is explicitly required.
- If an update needs a larger migration, call that out instead of hiding it inside a routine refresh.

## Pull Request Checklist

- [ ] Tests updated or added when behavior changed
- [ ] Docs updated if behavior or usage changed
- [ ] README updated if the public API changed
- [ ] Validation checks passing
