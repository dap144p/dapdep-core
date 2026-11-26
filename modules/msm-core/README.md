# MSM Core

`modules/msm-core` is the Minecraft-independent Java domain module used by Multi Sessions Manager (MSM).

It is maintained in the [Dapdep Core](https://github.com/dap144p/dapdep-core) repository and consumed by [MSM-Fabric](https://github.com/dap144p/Multi-Sessions-Manager-Fabric) as a pinned Git submodule. The two Minecraft integrations compile against the same module source.

## Responsibilities

| Class | Responsibility |
|---|---|
| `profile.SessionAuthMode` | Authentication-mode metadata (`MICROSOFT`, `OFFLINE_LOCAL`) |
| `profile.SessionProfile` | Immutable, validated account/server profile |
| `profile.SessionProfileStore` | Ordered server-profile persistence using Java Properties |
| `profile.AccountIdentity` | Immutable, validated account identity independent of a server |
| `profile.AccountIdentityStore` | Ordered account-identity persistence using Java Properties |
| `session.SessionManager` | In-memory registry, lookup, snapshots, and lifecycle transition validation |
| `session.SessionState` | Session lifecycle state model |

## Behavior and invariants

### SessionProfile

- Username is trimmed and must match `[A-Za-z0-9_]{1,16}`.
- Server address is trimmed and must contain 1–255 characters.

### AccountIdentity

- Stores a validated username and authentication mode without requiring a server address.
- Uses the same 1–16 character username validation as `SessionProfile`.
- Is persisted separately from server profiles so accounts can exist with zero configured servers.

### AccountIdentityStore

- Stores an ordered list of account identities in `msm-accounts.properties`.
- Missing file loads as an empty list; malformed entries raise an `IOException`.
- Uses temporary-file writes and atomic replacement when supported.
- Does not store credentials or tokens.
- Authentication mode is mandatory.
- The legacy two-argument constructor remains available and defaults to `MICROSOFT`.
- The value is immutable and implements equality/hash semantics across all profile fields.

### SessionProfileStore

- Stores an ordered list in Java Properties format.
- Missing profile file loads as an empty list.
- A malformed count, invalid profile, or inconsistent stored entry raises an `IOException`.
- Missing legacy `authMode` values default to `MICROSOFT`; explicit blank/unknown values are rejected.
- Saving creates parent directories, writes a temporary file, and attempts atomic replacement before falling back when atomic moves are unsupported.
- Credentials and tokens are not part of the persisted schema.

### SessionManager

- Maintains insertion order and generates in-memory numeric IDs.
- Rejects null and duplicate profiles.
- Provides lookup by ID/profile and immutable snapshots.
- Synchronizes registry operations.
- Allows only the transitions defined by `SessionState`.
- Prevents removal while a session is `CONNECTING` or `ONLINE`.
- Does not create threads, sockets, Minecraft clients, or reconnect jobs.

## Lifecycle transition table

| From | To |
|---|---|
| `OFFLINE` | `CONNECTING` |
| `CONNECTING` | `ONLINE`, `DISCONNECTED`, `FAILED` |
| `ONLINE` | `DISCONNECTED`, `FAILED` |
| `DISCONNECTED` | `CONNECTING` |
| `FAILED` | `CONNECTING` |

## Explicit exclusions

This module does not implement:

- Microsoft OAuth or account login.
- Password, access-token, or cookie storage.
- Minecraft protocol codecs, sockets, packet handling, or server authentication.
- Fabric entrypoints, mixins, screens, input handling, or rendering.
- AutoChat, AutoReconnect, connection scheduling, or game-version adapters.

Those responsibilities belong to MSM-Fabric or a separately designed integration module.

## Compatibility

- Package namespace: `dev.cylox.msm` (retained for existing source compatibility).
- Java source/target and compiler API release: Java 8.
- No Minecraft, Fabric, auth-provider, or network dependencies.
- Existing profile schema remains compatible with legacy records that omit `authMode`.

## Build and verification

Requirements: JDK 17 and Gradle 8.7 (or compatible Gradle 8.x). The module compiles with Java 8 API compatibility.

From the Dapdep Core repository root:

```sh
gradle --no-daemon :modules:msm-core:clean :modules:msm-core:check
```

The module's `check` task compiles tests and runs the profile-store and session-manager verification entry points.

## Consumer integration

MSM-Fabric includes this directory as Gradle project `:msm-core` from its `dapdep-core` Git submodule. Both supported Minecraft builds package the module classes into their own final JAR. Players install only the matching MSM-Fabric JAR; no separate Core JAR is required.

See [Dapdep Core Architecture](../../ARCHITECTURE.md) and the [MSM-Fabric README](https://github.com/dap144p/Multi-Sessions-Manager-Fabric#architecture-and-core-integration).
