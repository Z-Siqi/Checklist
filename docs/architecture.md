# Architecture Reference

This document preserves structural decisions that are too important to omit from `AGENTS.md` but too detailed to load for every task. It defines conventions, not an inventory of current implementation files.

## 1. Design Goals

- Keep Gradle modules coarse-grained to limit build and maintenance overhead.
- Use packages and visibility to enforce boundaries inside a feature module.
- Align shared feature contracts and implementations with platform presentation subfeatures.
- Prefer stable responsibility-based rules over lists of classes or files.

## 2. Module Roles

### App module

The app is the Android shell. It owns Compose presentation, navigation, Android lifecycle integration, startup, and platform-specific glue. It consumes shared functionality through the umbrella module.

### Umbrella module

The umbrella module is the shared export and assembly boundary for app targets. Changes to its exported surface can affect Android, iOS, and future clients, so they require compatibility review.

### Core modules

- `core:common`: cross-cutting utilities, result/error infrastructure, dispatching, and similar feature-independent code.
- `core:model`: pure shared domain models used by multiple features.
- `core:data`: persistence, files, database access, repositories, and related implementations.

Core code must not depend on features or app code. Do not move single-feature behavior into core merely to make it reusable from one caller.

### Feature modules

Each major product feature is one Gradle module. Its source uses two package-level boundaries:

- `api`: contracts, caller-visible state and models, and provider/factory entry points.
- `impl`: implementations and feature-private helpers.

`api` and `impl` must remain packages within the feature module unless a future architecture change is explicitly approved.

## 3. Subfeature Convention

The first business package beneath a feature boundary represents a subfeature:

```text
<feature>/api/<subfeature>/
<feature>/impl/<subfeature>/
presentation/<feature>/<subfeature>/
```

These paths describe logical alignment, not a requirement for identical internal directory trees. A subfeature may add deeper packages for cohesive responsibilities without creating more Gradle modules.

Use a new subfeature when behavior has a distinct entry point and cohesive responsibility. Do not create one merely for a single class, composable, dialog, helper, or data type.

Module-level providers may live at the `api` root when they assemble or expose several subfeature contracts. Providers return API types; concrete implementations remain internal.

## 4. Presentation Convention

Presentation is grouped first by feature and then subfeature. The subfeature root is its presentation boundary:

```text
presentation/<feature>/<subfeature>/
|-- <Subfeature>Layout.kt
|-- <Subfeature>ViewModel.kt
|-- <Subfeature>State.kt       optional
|-- <Subfeature>Request.kt     optional
`-- <responsibility>/...       internal implementation
```

The suffixes define roles:

- `*Layout.kt` is the main entry point exposed to navigation or another presentation assembly layer. It wires the ViewModel, observes state, renders the subfeature, and translates communication at the boundary. It must not become a general container for internal UI implementations.
- `*ViewModel.kt` owns presentation state and lifecycle-aware coordination with framework APIs. It must not own navigation or render UI.
- `*State.kt` is optional inbound communication: an external owner sets configuration, commands, or requested state for the subfeature to consume. It is not the ViewModel's complete observable UI state by default.
- `*Request.kt` is optional outbound communication: the subfeature asks its external owner or assembly layer to perform work it does not own, including navigation, opening another subfeature, or acknowledging completion. A request must not be consumed directly by an unrelated subfeature.
- Every other concrete implementation belongs in a child package grouped by responsibility, such as `scene`, `dialog`, `item`, or `type`.

These names define boundary semantics, not mandatory files. Do not create empty `State` or `Request` types when no corresponding communication exists. Prefer one main `Layout` and one `ViewModel` per subfeature; split only when the subfeature genuinely has multiple independent entry points.

Screen-level layouts assemble UI and connect state. ViewModels translate presentation intent into feature API calls. Fine-grained composables should be stateless where practical and remain inside the owning subfeature unless genuine app-wide reuse exists.

Navigation calls presentation entry points and handles routes or transitions. It must not own feature business state or construct feature behavior that belongs in presentation or framework code.

### 4.1 Internal package contract

Kotlin has no package-private visibility, and `internal` declarations appear throughout the Gradle module. Files below a presentation subfeature root are nevertheless package-scoped by convention.

- A non-private internal declaration intended only for its package and descendants must have this exact KDoc warning:

```kotlin
/** This method expected to be called only within this package and its sub-packages. **/
```

- Apply the warning to the callable or type that forms the internal package boundary. Private declarations do not need it.
- Code outside the declaration's package tree must not call it merely because Kotlin permits module-wide `internal` access.
- If a caller outside that tree genuinely needs the behavior, promote an intentional entry point at the subfeature root instead of bypassing the boundary.

### 4.2 Scoped internal UI APIs

The package warning documents the boundary but does not stop Android Studio from suggesting every module-wide internal callable. For internal UI helpers likely to pollute global completion, use a marker receiver declared with the subfeature layout:

```kotlin
internal interface <Subfeature>Scope

@Composable
internal fun <Subfeature>Scope.<InternalUi>(...) { ... }
```

- Name the marker after the subfeature, for example `<Subfeature>Scope`, and declare it in the corresponding `*Layout.kt` file.
- Make scoped helper functions extensions on that marker. They are available only where a scope receiver is deliberately provided, which reduces unrelated completion suggestions.
- Keep the scope capability-free: it is an IDE/API-boundary marker, not a service locator, dependency container, or mutable state holder.
- Create or provide the receiver only inside the subfeature's composition/assembly path. Do not expose it to navigation or unrelated subfeatures.
- Scope receivers complement `private`/`internal` visibility and the KDoc warning; they do not replace either.
- Plain `private` helpers and small data holders that do not create completion noise need no scope receiver.

## 5. API and Visibility

- Start with `private`, then `internal`; use public visibility only for a real cross-module consumer.
- Public API contains contracts and stable caller-facing data, never concrete implementation types.
- Database entities, DAOs, migrations, file implementations, and assembly details should not become feature API by convenience.
- Avoid widening visibility to solve a package-placement problem; revisit ownership first.
- When changing a public or umbrella-exported type, identify affected targets and preserve source/binary compatibility where practical.

## 6. Dependency Decisions

Allowed high-level flow:

```text
app -> umbrella -> features/core
feature implementation -> same feature API + core
feature API -> only required core contracts/types
core -> no app or feature code
```

A feature implementation must not call another feature implementation. Cross-feature workflows belong in an upper assembly layer or behind a deliberately shared contract. Avoid circular ownership disguised through callbacks, global state, or service locators.

## 7. Placement Decision Order

Apply the first matching rule:

1. Platform UI or interaction belongs in app presentation.
2. Platform lifecycle or OS integration belongs in app platform glue.
3. External feature contract belongs in that feature's API package.
4. Single-feature behavior belongs in that feature's implementation package.
5. Persistence and I/O implementation belongs in core data.
6. Pure models or utilities genuinely shared by multiple features belong in the appropriate core module.
7. Mixed code must be separated by responsibility before placement.

## 8. Documentation Maintenance

- Keep `AGENTS.md` below 200 lines and limited to frequently needed rules.
- Keep this reference independent of concrete implementation filenames and volatile inventories.
- Update this document when an approved architecture decision changes; do not update it for ordinary class or package-internal refactors.
- Prefer one durable rule over several examples. Add examples only when the rule would otherwise be ambiguous.
