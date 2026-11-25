# Dapdep Core Architecture

This document describes the current architecture and the rules for extending it. It distinguishes implemented components from future integration possibilities.

## 1. System boundary

Dapdep Core is a **shared source/build foundation**, not a process that every Dapdep application must run and not a requirement that all projects use the same language or framework.

```text
Dapdep ecosystem
|
+-- Dapdep Core (shared repository)
|   |
|   +-- modules/
|       |
|       +-- msm-core/       [implemented Java domain module]
|       +-- future modules  [add only when reusable and justified]
|
+-- MSM-Fabric              [current Core consumer]
|   |
|   +-- Minecraft 1.14.4 integration (repository root)
|   +-- Minecraft 1.21.8 integration (fabric-1.21.8/)
|   +-- UI, mixins, networking, automation, game-version adapters
|
+-- Cylox-Clients           [separate application; not yet a Core consumer]
+-- Nex-PlayBack            [separate Android application; not yet a Core consumer]
+-- Dapdep web/tools        [separate applications; no Core module declared yet]
```

The diagram is a repository responsibility map, not a claim that all listed applications already depend on Core.

## 2. Dependency direction

The intended dependency direction is:

```text
Application / platform adapter  --->  Core module
```

Core modules must not import application classes. The application owns the adapter that connects Core's platform-independent model to its runtime.

Examples:

- MSM-Fabric owns Fabric entrypoints, screens, mixins, Minecraft networking, protocol libraries, AutoChat, AutoReconnect, and game-version-specific code.
- MSM Core owns profile validation/persistence and the in-memory session domain model.
- A future Android module would need a real reusable Android-independent or Android-specific contract before it is added; the existence of Nex-PlayBack alone does not justify adding it.
- Web projects should remain in their own stack unless a concrete shared library or service contract is identified.

## 3. Current module: `modules/msm-core`

### Responsibility

The module provides the small domain layer shared by the two MSM Minecraft integrations.

| Type | Responsibility |
|---|---|
| `SessionAuthMode` | Authentication-mode metadata: `MICROSOFT` or `OFFLINE_LOCAL`; it does not perform authentication |
| `SessionProfile` | Immutable validated username/server/auth-mode value |
| `SessionProfileStore` | Load/save ordered profiles using Java Properties |
| `SessionManager` | Thread-safe in-memory session registry and state update guard |
| `SessionState` | Allowed lifecycle states and transitions |

### Profile validation and persistence

- Usernames are trimmed and restricted to 1–16 ASCII letters, digits, or underscores.
- Server addresses are trimmed and must contain 1–255 characters.
- Authentication mode is required; legacy stored profiles without an `authMode` value default to `MICROSOFT`.
- An explicitly blank or unknown stored authentication mode is rejected instead of silently rewritten.
- Profiles are stored as indexed Java Properties entries, preserving list order.
- Saving writes a temporary file and attempts an atomic replacement; it falls back to a normal replacement if the filesystem does not support atomic moves.
- The store does not store passwords, Microsoft credentials, access tokens, or session cookies.

### Session lifecycle

Supported states:

- `OFFLINE`
- `CONNECTING`
- `ONLINE`
- `DISCONNECTED`
- `FAILED`

Allowed transitions:

| Current state | Allowed next state |
|---|---|
| OFFLINE | CONNECTING |
| CONNECTING | ONLINE, DISCONNECTED, FAILED |
| ONLINE | DISCONNECTED, FAILED |
| DISCONNECTED | CONNECTING |
| FAILED | CONNECTING |

The manager rejects invalid transitions, rejects duplicate profiles, returns immutable list snapshots, and prevents removal while a session is CONNECTING or ONLINE. It is an in-memory registry; it does not own sockets or reconnect policy.

### Compatibility contract

- Package namespace remains `dev.cylox.msm` for source compatibility with existing MSM-Fabric integrations.
- The two-argument `SessionProfile(username, serverAddress)` constructor remains available and defaults to `MICROSOFT`.
- The module compiles with Java 8 API/bytecode compatibility using `options.release = 8`.
- The module has no Minecraft, Fabric, authentication-provider, or network dependency.

## 4. MSM-Fabric integration

MSM-Fabric tracks Dapdep Core as a Git submodule at `dapdep-core`.

The submodule is pinned by the parent repository's gitlink. It is intentionally not a floating branch dependency: a parent commit identifies the exact Core source revision used for that build.

Both Gradle builds include the same module directory:

- Root build (Minecraft 1.14.4): `dapdep-core/modules/msm-core`
- Nested build (Minecraft 1.21.8): `../dapdep-core/modules/msm-core`

Each build declares the included project as `:msm-core`, compiles against it, and packages the module classes into its remapped mod JAR. This avoids a second player-installed Core mod and keeps each release artifact self-contained with respect to MSM Core classes.

### Update procedure

1. Select the Dapdep Core commit to consume.
2. Update the `dapdep-core` submodule checkout to that commit.
3. Confirm the parent repo's gitlink points to the intended commit.
4. Run the root Minecraft 1.14.4 `clean check build`.
5. Run `fabric-1.21.8` `clean check build`.
6. Confirm both remapped JARs contain the required `dev/cylox/msm` classes.
7. Commit the gitlink change and document any API or persistence-format change.
8. Keep the previous Core revision available through Git history for rollback.

## 5. Build and CI model

Dapdep Core CI:

- JDK 17
- Gradle 8.7
- `gradle --no-daemon clean check`

MSM-Fabric CI:

- Minecraft 1.14.4 build runs with JDK 17; produced mod bytecode targets Java 8.
- Minecraft 1.21.8 build runs with JDK 21; produced mod bytecode targets Java 21.
- Checkout uses recursive submodule initialization.
- Each version has its own `check build` task and its own output JAR.
- CI verifies required Core classes and selected nested runtime libraries in the final JAR.

A passing workflow proves the configured automated build/check pipeline passed for that commit. It does not replace launcher/device testing, online service testing, or in-game regression testing.

## 6. Security and data ownership

Core should hold domain data, not secrets. Authentication providers, credentials, tokens, secure storage, and network connections belong to a dedicated integration with an explicit security design.

The current MSM Core profile format contains username, server address, and authentication-mode metadata only. Do not extend it to persist passwords or access tokens.

## 7. Adding a future module

Before creating a module, answer all of these:

1. Is the behavior genuinely reused by at least two independent consumers, or is there a clear near-term shared contract?
2. Can it be separated from product UI and platform lifecycle?
3. Is the API small, stable, and testable?
4. Which Java/runtime baseline does it require?
5. How will existing consumers migrate and how will persisted data remain compatible?
6. Can the module build and verify independently?

If the behavior belongs to one application, keep it in that application. Do not create placeholder modules just to make the repository look universal.

## 8. Non-goals

Dapdep Core does not currently provide:

- A universal application framework.
- A shared UI toolkit across Minecraft, Android, and web.
- A common networking stack or authentication service.
- A universal release pipeline for every Dapdep repository.
- Automatic dependency updates in consumer repositories.
- A guarantee that every Dapdep project is already integrated.
