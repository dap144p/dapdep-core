# Dapdep Core

**Dapdep Core** is the shared development foundation for projects in the Dapdep ecosystem.

The repository is organized as a modular Gradle build. Platform-specific integrations remain in their own repositories; reusable, platform-independent components live here.

## Modules

| Module | Purpose | Runtime compatibility |
|---|---|---|
| `modules:msm-core` | MSM profile persistence and session lifecycle domain model | Java 8 bytecode |

## Build and verify

Requires JDK 17 and Gradle 8.7 or newer compatible Gradle 8.x.

```sh
gradle --no-daemon clean check
```

## Integration policy

- Keep Minecraft/Fabric UI, input, mixins, and version-specific networking in MSM-Fabric.
- Keep MSM-specific profile/session behavior in the MSM module.
- Add a component to the shared root only when it is genuinely reusable across independent Dapdep projects.
- Preserve existing module APIs unless a migration explicitly documents a breaking change.

## Ecosystem

- [Multi-Sessions-Manager-Fabric](https://github.com/dap144p/Multi-Sessions-Manager-Fabric)
- [Cylox-Clients](https://github.com/dap144p/Cylox-Clients)
- [Nex-PlayBack](https://github.com/dap144p/Nex-PlayBack)
