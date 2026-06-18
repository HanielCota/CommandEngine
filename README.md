# CommandEngine

[![CI](https://github.com/HanielCota/CommandEngine/actions/workflows/ci.yml/badge.svg)](https://github.com/HanielCota/CommandEngine/actions/workflows/ci.yml)
[![JitPack](https://jitpack.io/v/HanielCota/CommandEngine.svg)](https://jitpack.io/#HanielCota/CommandEngine)
[![Java](https://img.shields.io/badge/Java-25-blue.svg)](https://openjdk.org/projects/jdk/25/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

CommandEngine is a compile-time command framework for Java 25 applications and Minecraft server platforms. It generates Brigadier adapters during compilation, so runtime execution uses direct method calls instead of reflection.

The first supported platform is Paper/Purpur. The API and runtime are platform-agnostic, so additional platform adapters can reuse the same command classes.

> Current release: `v0.1.0-alpha.1`. This is an alpha release: it is suitable for experiments and controlled internal plugins, but public API compatibility is not guaranteed yet.

## Table of Contents

- [Highlights](#highlights)
- [Repository Layout](#repository-layout)
- [Requirements](#requirements)
- [Modules](#modules)
- [Installing from JitPack](#installing-from-jitpack)
- [Quickstart](#quickstart)
- [Paper Configuration](#paper-configuration)
- [Execution Model](#execution-model)
- [Telemetry](#telemetry)
- [Rate Limiting](#rate-limiting)
- [Testing](#testing)
- [Code Style](#code-style)
- [CI](#ci)
- [Documentation](#documentation)
- [Production Readiness](#production-readiness)
- [Roadmap](#roadmap)
- [Changelog](#changelog)
- [License](#license)

## Highlights

- Java 25 baseline.
- Gradle 9.5.0 wrapper.
- JPMS modules for API, runtime, processor, Paper platform, tests and example plugin.
- Compile-time adapter generation through an annotation processor.
- Brigadier-native command trees.
- Root aliases and nested subcommands.
- Permission checks at root and subcommand level.
- `@Sender`, `@Arg`, `@Flag`, `@Greedy`, `@Range`, `@Min`, `@Max`, `@Suggestions` and `@SuggestionProvider`.
- `String args[]` and `String[] args` support for command-style argument arrays.
- Virtual-thread async execution for `void` handlers by default.
- `@Execute(async = false)` for sync handlers.
- Caffeine-backed cache infrastructure.
- Caffeine-backed command rate limiting.
- Configurable user-facing messages.
- Runtime telemetry SPI and `LoggingCommandTelemetry`.
- Paper scheduler bridge for thread-confined callbacks.
- Paper config loading from `plugin.getConfig()`.
- Spotless + Palantir Java Format wired into the build.
- GitHub Actions CI with JDK 25.
- JaCoCo test reports.

## Repository Layout

```text
CommandEngine/
├── commandengine-api/              # Public annotations, SPIs and contracts
├── commandengine-runtime/          # Runtime facade, executors, registry, cache and rate limit
├── commandengine-processor/        # Java annotation processor and generated adapter renderers
├── commandengine-platform-paper/   # Paper/Purpur integration
├── commandengine-example-paper/    # Minimal Paper plugin using generated commands
├── commandengine-test/             # Test harness, local Brigadier adapter and integration commands
├── docs/                           # Architecture, decisions, quickstart and production checklist
├── gradle/                         # Gradle wrapper and version catalog
└── .github/workflows/ci.yml        # CI pipeline
```

## Requirements

- JDK 25.
- Gradle wrapper included in the repository.
- Paper API `1.21.4-R0.1-SNAPSHOT` for the Paper platform module.

Check your local Java version:

```powershell
java -version
```

Build everything:

```powershell
.\gradlew.bat spotlessApply build --stacktrace
```

On Linux/macOS:

```bash
./gradlew spotlessApply build --stacktrace
```

## Modules

| Module | Published | Purpose |
| --- | --- | --- |
| `commandengine-api` | Yes | Public annotations, metadata, source abstraction and SPIs. |
| `commandengine-runtime` | Yes | Registration, execution, telemetry, configuration, cache and rate limit. |
| `commandengine-processor` | Yes | Annotation processor that generates Brigadier adapters. |
| `commandengine-platform-paper` | Yes | Paper/Purpur bridge, native resolvers and config loading. |
| `commandengine-test` | Yes | Test harness and local Brigadier utilities. |
| `commandengine-example-paper` | No | Minimal plugin for manual Paper smoke tests. |

### `commandengine-api`

The stable-facing public API. It contains annotations, command metadata, result types, `CommandSource`, scheduler, telemetry and rate-limit SPIs.

Important packages:

- `com.hanielfialho.api.annotation`
- `com.hanielfialho.api.command`
- `com.hanielfialho.api.source`
- `com.hanielfialho.api.argument`
- `com.hanielfialho.api.message`
- `com.hanielfialho.api.rate`
- `com.hanielfialho.api.scheduler`
- `com.hanielfialho.api.telemetry`

### `commandengine-processor`

The annotation processor. It reads command annotations during compilation and generates direct-call adapters and adapter factories.

Generated adapters are responsible for:

- Building Brigadier nodes.
- Applying permissions.
- Extracting sender, arguments and flags.
- Running rate limit checks.
- Dispatching sync or async handlers.
- Sending configured messages for framework-level failures.

### `commandengine-runtime`

The runtime facade used by applications and platforms. It owns registration, generated factory discovery through `ServiceLoader`, virtual-thread execution, telemetry wrapping, registry management and built-in configuration.

Main entry points:

- `CommandEngine`
- `CommandEngineConfig`
- `LoggingCommandTelemetry`

### `commandengine-platform-paper`

Paper/Purpur integration. It adapts Bukkit/Paper command senders, registers Brigadier-backed commands, provides native argument resolvers and loads external configuration.

Main entry points:

- `PaperPlatform`
- `PaperCommandEngineConfigLoader`
- `PaperCommandSource`
- `PlayerArgumentResolver`
- `WorldArgumentResolver`
- `LocationArgumentResolver`
- `MaterialArgumentResolver`

### `commandengine-example-paper`

A minimal Paper plugin that proves the processor, runtime and Paper bridge can be used together in a real plugin layout.

Generated JAR:

```text
commandengine-example-paper/build/libs/commandengine-example-paper-0.1.0-SNAPSHOT.jar
```

Example commands:

- `/cexample ping`
- `/cexample echo <message>`

Build only the example plugin:

```powershell
.\gradlew.bat :commandengine-example-paper:build
```

The plugin JAR is created at:

```text
commandengine-example-paper/build/libs/commandengine-example-paper-0.1.0-SNAPSHOT.jar
```

## Installing from JitPack

Add JitPack to your repositories:

```kotlin
dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://jitpack.io")
  }
}
```

Use a Git tag, commit hash or `main-SNAPSHOT` as the version. Tags are recommended for reproducible builds:

```kotlin
val commandEngineVersion = "v0.1.0-alpha.1"

dependencies {
  implementation("com.github.HanielCota.CommandEngine:commandengine-api:$commandEngineVersion")
  implementation("com.github.HanielCota.CommandEngine:commandengine-runtime:$commandEngineVersion")
  annotationProcessor("com.github.HanielCota.CommandEngine:commandengine-processor:$commandEngineVersion")
}
```

For Paper:

```kotlin
val commandEngineVersion = "v0.1.0-alpha.1"

dependencies {
  implementation("com.github.HanielCota.CommandEngine:commandengine-api:$commandEngineVersion")
  implementation("com.github.HanielCota.CommandEngine:commandengine-runtime:$commandEngineVersion")
  implementation("com.github.HanielCota.CommandEngine:commandengine-platform-paper:$commandEngineVersion")
  annotationProcessor("com.github.HanielCota.CommandEngine:commandengine-processor:$commandEngineVersion")
}
```

Published module artifacts:

- `commandengine-api`
- `commandengine-runtime`
- `commandengine-processor`
- `commandengine-platform-paper`
- `commandengine-test`

The `commandengine-example-paper` module is intentionally not published as a library artifact.

### Private Repository Access

This repository is currently private. To consume it through JitPack, authorize JitPack for your GitHub account and add
your JitPack token to `$HOME/.gradle/gradle.properties`:

```properties
authToken=AUTHENTICATION_TOKEN
```

Then configure the JitPack repository with credentials:

```kotlin
maven {
  url = uri("https://jitpack.io")
  credentials.username = providers.gradleProperty("authToken").get()
}
```

For public repositories, credentials are not needed.

## Quickstart

### Gradle Dependencies

For a generic Brigadier-backed application:

```kotlin
dependencies {
  implementation(project(":commandengine-api"))
  implementation(project(":commandengine-runtime"))
  annotationProcessor(project(":commandengine-processor"))

  testImplementation(project(":commandengine-test"))
  testAnnotationProcessor(project(":commandengine-processor"))
}
```

For Paper:

```kotlin
dependencies {
  implementation(project(":commandengine-api"))
  implementation(project(":commandengine-runtime"))
  implementation(project(":commandengine-platform-paper"))
  annotationProcessor(project(":commandengine-processor"))
}
```

### Define a Command

```java
package com.hanielfialho.example;

import com.hanielfialho.api.annotation.Arg;
import com.hanielfialho.api.annotation.Command;
import com.hanielfialho.api.annotation.Greedy;
import com.hanielfialho.api.annotation.Range;
import com.hanielfialho.api.annotation.Sender;
import com.hanielfialho.api.annotation.Subcommand;
import com.hanielfialho.api.annotation.SuggestionProvider;
import com.hanielfialho.api.annotation.Suggestions;
import com.hanielfialho.api.source.CommandSource;
import java.util.List;

@Command(
        name = "warp",
        aliases = {"w"},
        permission = "warp.use",
        description = "Warp management command")
public final class WarpCommand {

    @Subcommand("teleport")
    public void teleport(@Sender CommandSource source, @Arg("name") @Suggestions("warpNames") String warpName) {
        source.sendMessage("Teleporting to " + warpName);
    }

    @Subcommand(value = "create", permission = "warp.admin")
    public void create(
            @Sender CommandSource source,
            @Arg("name") String name,
            @Arg("cost") @Range(min = 0, max = 10_000) int cost) {
        source.sendMessage("Created warp " + name + " with cost " + cost);
    }

    @Subcommand(value = "broadcast", permission = "warp.admin")
    public void broadcast(@Arg("message") @Greedy String message) {
        // Send through your plugin service.
    }

    @SuggestionProvider("warpNames")
    public List<String> warpNames() {
        return List.of("spawn", "shop", "arena");
    }
}
```

### Register Commands in Paper

```java
package com.hanielfialho.example;

import com.hanielfialho.platform.paper.PaperPlatform;
import com.hanielfialho.runtime.CommandEngine;
import org.bukkit.plugin.java.JavaPlugin;

public final class ExamplePlugin extends JavaPlugin {

    private CommandEngine engine;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        engine = CommandEngine.create(PaperPlatform.create(this));
        engine.register(new WarpCommand());
    }

    @Override
    public void onDisable() {
        if (engine == null) {
            return;
        }

        engine.close();
        engine = null;
    }
}
```

## Paper Configuration

`PaperPlatform.create(plugin)` loads `plugin.getConfig()` automatically. Missing values use safe defaults from `CommandEngineConfig`.

```yaml
commandengine:
  async-timeout-millis: 30000
  messages:
    internal-error: "An internal error occurred while executing this command."
    invalid-sender: "This command cannot be executed by this sender."
    invalid-syntax: "Invalid command syntax."
    no-permission: "You do not have permission to execute this command."
    rate-limited: "You are executing this command too quickly."
  rate-limit:
    window-millis: 2000
    max-executions: 5
    maximum-size: 10000
```

Manual loading is also supported:

```java
var config = PaperCommandEngineConfigLoader.load(getConfig());
var platform = PaperPlatform.create(this, config);
var engine = CommandEngine.create(platform);
```

## Execution Model

CommandEngine separates command parsing from command execution:

1. Brigadier parses the command.
2. The generated adapter validates permission and rate limit.
3. The adapter extracts sender, arguments and flags.
4. The adapter calls the user command method directly.
5. Void handlers run on virtual threads by default.
6. Framework messages are sent through the configured `CommandScheduler`.

Handlers returning `int` remain synchronous because Brigadier needs the result immediately.

Use sync execution explicitly:

```java
@Execute(async = false)
@Subcommand("reload")
public void reload(@Sender CommandSource source) {
    source.sendMessage("Reloaded.");
}
```

## Telemetry

Telemetry is opt-in through the `CommandTelemetry` SPI.

```java
var telemetry = new LoggingCommandTelemetry(Logger.getLogger("CommandEngine"));

var engine = CommandEngine.builder()
        .brigadier(brigadierAdapter)
        .telemetry(telemetry)
        .build();
```

The runtime records:

- Command execution duration.
- Failure reason.
- Async/sync execution mode.
- Suggestion generation duration and count.

## Rate Limiting

The public SPI is `CommandRateLimiter`.

```java
CommandRateLimiter limiter = (source, path) -> true;
```

The default config path uses a Caffeine-backed limiter:

```java
var config = CommandEngineConfig.defaults();
var engine = CommandEngine.builder()
        .brigadier(brigadierAdapter)
        .config(config)
        .build();
```

Paper loads the same configuration from `config.yml`.

## Testing

Run all tests:

```powershell
.\gradlew.bat test
```

Run build, formatting and tests:

```powershell
.\gradlew.bat spotlessApply build --stacktrace
```

The test suite covers:

- Processor generation with compile-testing.
- Generated adapter integration through a real Brigadier dispatcher.
- Permission checks.
- Aliases.
- Greedy arguments.
- Flags.
- External argument resolvers.
- Configurable messages.
- Rate limiting.
- Paper bridge permission, syntax, internal error and tab-complete behavior.
- Paper config loading.
- Basic high-volume dispatch smoke test.

## Code Style

The build uses Spotless with Palantir Java Format:

```powershell
.\gradlew.bat spotlessApply
```

Project conventions:

- Javadocs are written in English.
- Public APIs use JetBrains nullability annotations where appropriate.
- Prefer records for immutable data.
- Prefer early return.
- Do not use `else` in Java source or generated adapters.
- Do not use `break` or `continue` in Java source or generated adapters.
- Keep platform-specific code outside the core runtime.
- Keep `*.internal.*` packages out of public API usage.

## CI

GitHub Actions runs:

```bash
./gradlew spotlessCheck build --stacktrace
```

Workflow:

```text
.github/workflows/ci.yml
```

## Releases

Releases are tagged with semantic pre-release versions, for example `v0.1.0-alpha.1`.

Release checklist:

1. Run `.\gradlew.bat spotlessApply build publishToMavenLocal --stacktrace`.
2. Confirm `git status --short --branch` is clean.
3. Create an annotated tag, for example `git tag -a v0.1.0-alpha.1 -m "v0.1.0-alpha.1"`.
4. Push the tag with `git push origin v0.1.0-alpha.1`.
5. Create a GitHub release from the tag.
6. Open JitPack and request the tag build.

## Documentation

- [Quickstart](docs/QUICKSTART.md)
- [Architecture](docs/ARCHITECTURE.md)
- [Decision Log](docs/DECISION_LOG.md)
- [Coding Standards](docs/CODING_STANDARDS.md)
- [Production Checklist](docs/PRODUCTION_CHECKLIST.md)
- [Changelog](CHANGELOG.md)

## Production Readiness

The project is close to internal controlled production use, but still alpha for public consumption.

Before a public release:

- Run the example plugin on a real Paper server.
- Validate command registration, aliases, reload and disable behavior.
- Validate tab-complete in game.
- Validate async handlers do not call thread-confined Paper APIs directly.
- Decide the stable public API surface for `commandengine-api`.
- Add release publishing and versioning automation.
- Add broader platform coverage or explicitly scope the first release to Paper.

Use [Production Checklist](docs/PRODUCTION_CHECKLIST.md) before shipping.

## Roadmap

- Micrometer telemetry module.
- Graceful suggestion degradation.
- More Paper integration tests.
- Public Javadoc publishing.
- Gradle plugin for command tree inspection and validation.
- Velocity platform adapter.
- Fabric platform adapter.
- Sponge platform adapter.
- Benchmark suite.

## Changelog

See [CHANGELOG.md](CHANGELOG.md).

## License

MIT. See [LICENSE](LICENSE).
