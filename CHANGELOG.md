# Changelog

All notable changes to CommandEngine are documented here.

The project follows semantic versioning once the public API becomes stable. Alpha releases may still contain breaking API changes.

## v0.1.0-alpha.5 - 2026-06-19

### Added

- Added compile-time validation that rejects `@Command` classes without a root handler or subcommand.
- Added case-insensitive validation for command aliases, including duplicate aliases and aliases equal to the command name.
- Added telemetry coverage for generated suggestion providers.
- Added deterministic rate limiter tests through an injectable Caffeine ticker.
- Added regression tests for telemetry callback failures, unregister cleanup and Paper tab completion timeouts.

### Changed

- Improved generated adapter helpers so optional argument detection uses parsed Brigadier nodes instead of exception-driven lookups.
- Improved generated vararg/list argument splitting to avoid regex splitting and preserve predictable whitespace handling.
- Improved runtime factory discovery by making factory caches instance-scoped instead of global per class loader.
- Improved runtime executor setup so telemetry is skipped when `CommandTelemetry.NOOP` is configured.
- Improved lifecycle cleanup so adapter unregister failures do not leave registry entries behind.
- Improved Paper unregister handling for aliases, canonical command names and normalized labels.
- Reduced Paper tab completion wait time to keep stalled suggestion futures from blocking server completion for several seconds.
- Updated test harnesses to close engines consistently.

### Fixed

- Fixed telemetry callback exceptions so they no longer fail successful command execution.
- Fixed scheduler callbacks on Paper so disabled plugins do not enqueue or run delayed command callbacks.
- Fixed interrupt-sensitive async timeout tests to avoid sleep timing flakiness.
- Fixed JPMS module declarations for downstream consumers that require API/runtime transitively.
- Fixed documentation around Spotless build behavior.

### Notes

- This is an alpha release. Public API compatibility is not guaranteed.
- Local validation passed with `.\gradlew.bat spotlessApply spotlessCheck build --stacktrace`.

## v0.1.0-alpha.4 - 2026-06-19

### Added

- Added an agent guide in `AGENTS.md` with project context, module responsibilities, validation commands and CI/SonarCloud notes.
- Added broader runtime and Paper platform tests for command registration, unregister behavior, rate limiting, native arguments and plugin disable cleanup.

### Changed

- Refreshed the README with a fuller modern project overview, install instructions, quickstart, architecture summary and contribution guidance.
- Improved command registration and unregister cleanup so adapters, Brigadier nodes and platform command map entries remain consistent.
- Improved generated adapter handling for optional arguments, flags, string validation, custom argument resolution and telemetry-wrapped execution.
- Improved Paper command binding, command claiming, tab completion timeout handling and native argument/suggestion behavior.
- Improved virtual-thread execution timeout handling and lifecycle cleanup.

### Fixed

- Fixed SonarCloud reliability issues around nullable values and exception masking during close/cleanup.
- Fixed rate limiter counter updates to avoid nullable `Map.compute` results.
- Fixed Spotless formatting regressions in the Paper platform binding.

### Notes

- This is an alpha release. Public API compatibility is not guaranteed.
- GitHub Actions and SonarCloud are green for this release commit.

## v0.1.0-alpha.3 - 2026-06-18

### Fixed

- Applied required Spotless formatting after the public alpha release.

### Notes

- No intentional runtime, API or processor behavior changed from `v0.1.0-alpha.2`.

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
