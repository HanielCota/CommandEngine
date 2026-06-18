# Changelog

All notable changes to CommandEngine are documented here.

The project follows semantic versioning once the public API becomes stable. Alpha releases may still contain breaking API changes.

## v0.1.0-alpha.2 - 2026-06-18

### Changed

- Made the GitHub repository public.
- Updated JitPack documentation to remove private repository credentials.
- Updated release documentation to use public JitPack coordinates.

### Notes

- No runtime, API or processor behavior changed from `v0.1.0-alpha.1`.
- This is an alpha release. Public API compatibility is not guaranteed.

## v0.1.0-alpha.1 - 2026-06-18

### Added

- Java 25 multi-module Gradle project.
- Public API module with command annotations, metadata, result types, sources, schedulers, telemetry and rate-limit SPIs.
- Annotation processor that generates direct-call Brigadier adapters and factories.
- Runtime module with command registration, generated factory discovery, virtual-thread execution and configurable messages.
- Caffeine-backed cache infrastructure.
- Caffeine-backed command rate limiting through `CommandRateLimiter`.
- `CommandEngineConfig` for runtime messages, async timeout and rate-limit settings.
- `LoggingCommandTelemetry` implementation.
- Paper/Purpur platform module with command bridge, source adapter, scheduler, unregister handling and native resolvers.
- Paper config loader backed by Bukkit `FileConfiguration`.
- Minimal Paper example plugin.
- Integration tests for generated adapters, permissions, aliases, flags, greedy arguments, external resolvers, telemetry, custom messages and rate limiting.
- Paper tests for bridge failures, permissions, tab completion, config loading, suggestions and player resolution.
- GitHub Actions CI with JDK 25.
- Spotless with Palantir Java Format.
- JitPack publishing configuration.

### Notes

- This is an alpha release. Public API compatibility is not guaranteed.
- The repository is public, so JitPack consumers do not need private repository authorization.
