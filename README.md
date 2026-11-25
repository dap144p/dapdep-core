# Dapdep Core

**Dapdep Core** is the shared engineering foundation for the Dapdep project ecosystem. It provides a modular home for reusable code, common build conventions, verification, and integration contracts.

The repository is intentionally **not a universal runtime**. A component belongs here only when it has a clear reusable responsibility and can remain independent of a consuming application's platform. Minecraft/Fabric client code, Android UI, web endpoints, and product-specific behavior stay in their respective application repositories.

> **Current implementation status:** the repository currently contains the extracted, Minecraft-independent MSM domain module. Other Dapdep projects are not yet migrated to Core modules.

## Repository map

| Path | Responsibility | Status |
|---|---|---|
| `modules/msm-core/` | MSM profile model, profile persistence, session registry, and lifecycle state model | Active; consumed by MSM-Fabric |
| `settings.gradle` | Declares the Gradle multi-project structure | Active |
| `build.gradle` | Shared Gradle group/version defaults | Active |
| `.github/workflows/check.yml` | Runs Core build and verification on pushes, pull requests, and manual dispatch | Active |
| `ARCHITECTURE.md` | Module boundaries, dependency direction, and integration model | Documentation |

## Architecture principles

1. **Modular, not monolithic.** Each capability has its own module and explicit responsibility.
2. **Platform boundaries stay outside Core.** A reusable module must not depend on Minecraft, Fabric, Android framework APIs, browser APIs, or a specific product UI unless that module is explicitly platform-scoped.
3. **Applications own integration.** Applications adapt Core APIs to their runtime, UI, storage location, networking, and lifecycle.
4. **No forced shared stack.** Java, Android, web, and tooling projects may use different build systems and languages. They integrate through a suitable module, library, service contract, or generated artifact—not by forcing every project into one Gradle build.
5. **Compatibility is deliberate.** Existing public APIs and persisted formats are preserved unless a migration explicitly documents a breaking change.
6. **Verification is part of the module.** Core changes must include relevant tests or executable checks and pass the repository workflow.

See [ARCHITECTURE.md](ARCHITECTURE.md) for the full dependency and integration model.

## Current module: MSM Core

The `modules/msm-core` module contains the shared Java domain layer used by Multi Sessions Manager.

It currently provides:

- Validated `SessionProfile` values with username, server address, and authentication-mode metadata.
- `SessionProfileStore` for Java Properties persistence, temporary-file writes, and atomic replacement when supported.
- `SessionManager` for an ordered in-memory session registry, lookup, immutable snapshots, and guarded removal.
- `SessionState` for explicit lifecycle states and validated transitions.
- Java 8-compatible bytecode.

It deliberately does **not** implement Microsoft sign-in, credential/token storage, socket connections, Minecraft packet handling, reconnect scheduling, AutoChat, Fabric UI, or world rendering. Those responsibilities remain in MSM-Fabric.

Detailed API and compatibility notes: [modules/msm-core/README.md](modules/msm-core/README.md).

## Build and verification

Requirements:

- JDK 17
- Gradle 8.7 or a compatible Gradle 8.x release

The repository currently does not include a Gradle Wrapper. Run with an installed Gradle:

```sh
gradle --no-daemon clean check
```

To verify only MSM Core:

```sh
gradle --no-daemon :modules:msm-core:clean :modules:msm-core:check
```

The module compiles with `--release 8`. Its `check` lifecycle runs the profile-store and session-manager verification entry points. The GitHub Actions workflow runs the root `clean check` task on pushes, pull requests, and manual dispatch.

## Consuming Core from another repository

MSM-Fabric currently consumes this repository as a Git submodule pinned to a specific commit. This gives the application a reproducible Core revision rather than silently tracking a moving branch.

```sh
git clone --recurse-submodules https://github.com/dap144p/Multi-Sessions-Manager-Fabric.git
```

For an existing clone:

```sh
git submodule update --init --recursive
```

The MSM-Fabric Gradle settings include `dapdep-core/modules/msm-core` as the `:msm-core` project. The consuming mod packages the module classes into its final version-specific JAR; players do not install a separate Core mod.

To upgrade the Core revision, update the submodule checkout to the intended Dapdep Core commit, validate both MSM targets, and commit the changed gitlink in MSM-Fabric. See [ARCHITECTURE.md](ARCHITECTURE.md).

## Compatibility and stability

- The MSM Core module targets Java 8 bytecode.
- The Core repository build itself uses JDK 17 and Gradle 8.7.
- A successful Core check proves the module's current automated checks pass; it does not prove every consuming application works at runtime.
- MSM-Fabric must continue to build and test each supported Minecraft target after a Core change.
- Persisted profile compatibility and public Java API compatibility are treated as migration constraints.

## Dapdep ecosystem

- [Multi-Sessions-Manager-Fabric](https://github.com/dap144p/Multi-Sessions-Manager-Fabric) — current consumer of `modules/msm-core`.
- [Cylox-Clients](https://github.com/dap144p/Cylox-Clients) — separate Minecraft client project; Core integration is not yet documented as active.
- [Nex-PlayBack](https://github.com/dap144p/Nex-PlayBack) — separate Android project; Core integration is not yet documented as active.

## Contribution rules

- Inspect the existing module and its consumers before changing an API or data format.
- Keep changes scoped to the module that owns the behavior.
- Do not move application-specific UI, networking, or platform lifecycle code into Core merely to centralize files.
- Add or update verification when behavior changes.
- Update module documentation and consumer integration notes when public contracts change.
- Do not claim runtime compatibility based only on compilation.
