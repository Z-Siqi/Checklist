# Checklist Agent Guidelines

## Defaults

- Use Kotlin as the primary language.
- Use Jetpack Compose for Android UI and SwiftUI for iOS UI.
- Prefer small, explicit, production-ready changes over clever abstractions.
- Use `private` or `internal` by default. Expose only the minimum API required across Gradle modules.
- Keep replies direct and concise; explain non-obvious architectural decisions briefly.
- Preserve unrelated user changes and avoid opportunistic refactors.

## Architecture Summary

The project uses coarse-grained Gradle modules:

```text
:app
:framework:umbrella
:framework:core:<area>
:framework:feature:<feature>
```

Within a feature Gradle module, `api` and `impl` are packages, not submodules. A feature's first business package represents a subfeature. Keep the same subfeature name aligned across framework API, framework implementation, and app presentation code.

Example shape only:

```text
framework/feature/<feature>/.../<feature>/api/<subfeature>/
framework/feature/<feature>/.../<feature>/impl/<subfeature>/
app/.../presentation/<feature>/<subfeature>/
```

Do not create a Gradle module for each feature API, implementation, subfeature, screen, or composable.

## Dependency Rules

```text
:app -> :framework:umbrella
:framework:umbrella -> core modules + feature modules
feature modules -> required core modules
core modules -> no feature or app dependencies
```

- Never introduce reverse or circular dependencies.
- An `api` package must not depend on its `impl` package.
- Features must not depend on another feature's implementation.
- Implementation types must not leak through public signatures.
- Coordinate feature-to-feature behavior through contracts or upper-layer assembly.

## Placement Rules

| Responsibility | Destination |
|---|---|
| UI, interaction, UI state, ViewModel | `app/.../presentation/<feature>/<subfeature>/` |
| Navigation, startup, Android platform glue | the corresponding `app` package |
| Feature contracts, exposed state, providers | feature `api` package |
| Single-feature business logic and helpers | feature `impl` package |
| Database, files, repositories | `framework/core:data` |
| Cross-platform shared models | `framework/core:model` |
| Cross-feature utilities and error types | `framework/core:common` |

- Keep subfeature entry points near the subfeature package root; group internal UI or helpers by responsibility below it.
- At a presentation subfeature root, use suffixes consistently: `Layout` is the main entry point, `ViewModel` owns state handling, optional `State` carries inbound instructions, and optional `Request` carries outbound requests. Put other implementation files in child packages.
- Internal presentation declarations in child packages must carry the package-use warning defined in `docs/architecture.md`.
- Use a `<Subfeature>Scope` receiver for internal UI helpers that would otherwise pollute module-wide code completion.
- Navigation owns routes and transitions, not business state or feature logic.
- Prefer stateless composables and hoisted state. Check accessibility, lifecycle, coroutine ownership, and adaptive layouts.
- Split mixed responsibilities before choosing a destination.

## Change Authority

Proceed directly with local implementations, bug fixes, UI adjustments, tests, and internal refactors that preserve module and package boundaries.

Ask for confirmation before:

- adding or removing Gradle modules;
- editing Gradle build structure or dependency direction;
- moving files across modules or reorganizing feature boundaries;
- renaming packages or introducing a new architecture pattern;
- changing `expect`/`actual` contracts;
- expanding public APIs or changing shared models;
- changing the umbrella module's exported surface.

If a request cannot be implemented cleanly within these constraints, explain the conflict instead of silently bypassing it.

## Execution and Validation

Before a non-trivial change, briefly state the affected files/modules, placement rationale, visibility impact, and architecture risk.

- Validate the narrowest affected module first, then expand according to risk.
- Prefer deterministic tests and cover relevant edge cases.
- Use the Gradle wrapper and select tasks from the current project configuration; do not assume a documented example task still exists.

## Detailed Reference

Read [`docs/architecture.md`](docs/architecture.md) only when a task affects module structure, package boundaries, dependency direction, public API, code placement, or presentation entry/communication conventions. For unrelated local changes, this file is sufficient; avoid loading extra documentation merely for context.
