# MSM Core module

This module contains the Minecraft-independent Java domain layer used by Multi Sessions Manager.

## Included

- Validated session profiles and authentication-mode metadata.
- Java Properties persistence with temporary-file writes and atomic replacement when supported.
- In-memory session registry, immutable snapshots, and validated lifecycle transitions.
- Java 8-compatible bytecode with no Minecraft, Fabric, authentication, or network dependency.

No passwords, Microsoft credentials, access tokens, or session cookies are stored. Microsoft sign-in, independent network connections, chat, reconnect logic, Minecraft UI, and session controls remain MSM-Fabric integration responsibilities.

## Compatibility

The module retains the existing `dev.cylox.msm` package and public API to avoid breaking current MSM-Fabric integrations.

## Verification

Run from the Dapdep Core repository root:

```sh
gradle :modules:msm-core:clean :modules:msm-core:check
```
