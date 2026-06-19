# CommandEngine

[![CI](https://github.com/HanielCota/CommandEngine/actions/workflows/ci.yml/badge.svg)](https://github.com/HanielCota/CommandEngine/actions/workflows/ci.yml)
[![JitPack](https://jitpack.io/v/HanielCota/CommandEngine.svg)](https://jitpack.io/#HanielCota/CommandEngine)
[![Java](https://img.shields.io/badge/Java-25-blue.svg)](https://openjdk.org/projects/jdk/25/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

CommandEngine e um framework de comandos compile-time para Java 25 e plataformas de servidores Minecraft. Ele gera
adapters Brigadier durante a compilacao, permitindo que comandos sejam executados com chamadas diretas aos metodos do
usuario, sem reflection no hot path.

O primeiro adapter oficial e para Paper/Purpur, mas o core foi desenhado para ser agnostico de plataforma. Novos
adapters podem reutilizar as mesmas anotacoes, o mesmo runtime e os mesmos contratos publicos.

> Release atual: `v0.1.0-alpha.5`. O projeto ainda esta em alpha: ja serve para experimentos e plugins controlados,
> mas a API publica ainda pode mudar antes da versao estavel.

## Destaques

- Java 25 e Gradle 9.5.0.
- API modular com JPMS.
- Annotation processor para gerar adapters Brigadier.
- Registro de comandos, aliases, permissoes e subcomandos.
- Suporte a `@Sender`, `@Arg`, `@Flag`, `@Optional`, `@Greedy`, `@Range`, `@Min`, `@Max`, `@Suggestions` e
  `@SuggestionProvider`.
- Suporte a argumentos `String[]` e `List<String>` para comandos estilo CLI.
- Handlers `void` executam em virtual threads por padrao.
- `@Execute(async = false)` para handlers sincronizados.
- Mensagens configuraveis para erros de framework.
- Rate limit com Caffeine.
- Telemetry SPI com implementacao de logging.
- Adapter Paper/Purpur com resolvers nativos para `Player`, `World`, `Location` e `Material`.
- Build com Spotless + Palantir Java Format.
- CI com GitHub Actions e JDK 25.
- Relatorios JaCoCo.

## Indice

- [Como Funciona](#como-funciona)
- [Modulos](#modulos)
- [Requisitos](#requisitos)
- [Instalacao](#instalacao)
- [Quickstart](#quickstart)
- [Uso no Paper](#uso-no-paper)
- [Configuracao Paper](#configuracao-paper)
- [Anotacoes](#anotacoes)
- [Execution Model](#execution-model)
- [Telemetry](#telemetry)
- [Rate Limiting](#rate-limiting)
- [Testes e Build](#testes-e-build)
- [Arquitetura](#arquitetura)
- [Documentacao](#documentacao)
- [Roadmap](#roadmap)
- [Status de Producao](#status-de-producao)
- [Contribuicao](#contribuicao)
- [Licenca](#licenca)

## Como Funciona

O CommandEngine separa definicao, geracao e execucao:

```text
Classe anotada
     |
     v
commandengine-processor
     |
     v
Adapter Brigadier gerado
     |
     v
CommandEngine runtime
     |
     v
Platform adapter, ex: Paper
```

Na pratica:

1. Voce escreve uma classe com `@Command`, `@Subcommand`, `@Arg`, `@Sender` e outras anotacoes.
2. O annotation processor valida essa classe em compile-time.
3. O processor gera um adapter Brigadier e uma factory.
4. O runtime localiza a factory com `ServiceLoader`.
5. A plataforma registra o comando e encaminha a execucao.
6. O adapter gerado extrai argumentos, valida permissao/rate limit e chama seu metodo diretamente.

## Modulos

| Modulo | Publicado | Responsabilidade |
| --- | --- | --- |
| `commandengine-api` | Sim | Anotacoes, metadata, contratos publicos e SPIs. |
| `commandengine-runtime` | Sim | Facade `CommandEngine`, registry, executors, config, telemetry, cache e rate limit. |
| `commandengine-processor` | Sim | Annotation processor e geracao dos adapters. |
| `commandengine-platform-paper` | Sim | Integracao Paper/Purpur, command bridge, resolvers e config loader. |
| `commandengine-test` | Sim | Harness, mocks e utilitarios para testes de commands. |
| `commandengine-example-paper` | Nao | Plugin Paper minimo para smoke tests manuais. |

Estrutura do repositorio:

```text
CommandEngine/
├── commandengine-api/
├── commandengine-runtime/
├── commandengine-processor/
├── commandengine-platform-paper/
├── commandengine-example-paper/
├── commandengine-test/
├── docs/
├── gradle/
├── .github/workflows/ci.yml
└── AGENTS.md
```

## Requisitos

- JDK 25.
- Gradle wrapper incluido no repositorio.
- Paper API `1.21.4-R0.1-SNAPSHOT` para o modulo Paper.

Verifique sua versao do Java:

```powershell
java -version
```

## Instalacao

### JitPack

Adicione os repositorios:

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

Use um tag, commit hash ou `main-SNAPSHOT`. Tags sao recomendadas para builds reproduziveis:

```kotlin
val commandEngineVersion = "v0.1.0-alpha.5"

dependencies {
    implementation("com.github.HanielCota.CommandEngine:commandengine-api:$commandEngineVersion")
    implementation("com.github.HanielCota.CommandEngine:commandengine-runtime:$commandEngineVersion")
    annotationProcessor("com.github.HanielCota.CommandEngine:commandengine-processor:$commandEngineVersion")

    testImplementation("com.github.HanielCota.CommandEngine:commandengine-test:$commandEngineVersion")
    testAnnotationProcessor("com.github.HanielCota.CommandEngine:commandengine-processor:$commandEngineVersion")
}
```

Para Paper, adicione:

```kotlin
implementation("com.github.HanielCota.CommandEngine:commandengine-platform-paper:$commandEngineVersion")
```

Como o repositorio e publico, consumidores nao precisam configurar credenciais do JitPack.

### Projeto Local

Para consumir os modulos dentro deste monorepo:

```kotlin
dependencies {
    implementation(project(":commandengine-api"))
    implementation(project(":commandengine-runtime"))
    annotationProcessor(project(":commandengine-processor"))

    testImplementation(project(":commandengine-test"))
    testAnnotationProcessor(project(":commandengine-processor"))
}
```

Para Paper:

```kotlin
dependencies {
    implementation(project(":commandengine-api"))
    implementation(project(":commandengine-runtime"))
    implementation(project(":commandengine-platform-paper"))
    annotationProcessor(project(":commandengine-processor"))
}
```

## Quickstart

Crie uma classe de comando:

```java
package com.example.commands;

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

Registre no runtime:

```java
var engine = CommandEngine.builder()
        .brigadier(brigadierAdapter)
        .owner(pluginOrApplication)
        .build();

engine.register(new WarpCommand());
```

### JPMS

Se seu modulo declara `module-info.java`, exponha a factory gerada:

```java
module com.example.myplugin {
    requires com.hanielfialho.api;
    requires com.hanielfialho.runtime;

    provides com.hanielfialho.api.command.CommandAdapterFactory
        with com.example.commands.WarpCommandCommandAdapterFactory;
}
```

Sem esse `provides`, `CommandEngine.register(...)` nao encontrara a factory gerada em runtime.

## Uso no Paper

Plugin minimo:

```java
package com.example;

import com.example.commands.WarpCommand;
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

O modulo Paper oferece:

- `PaperPlatform`
- `PaperCommandSource`
- `PaperCommandEngineConfigLoader`
- `PlayerArgumentResolver`
- `WorldArgumentResolver`
- `LocationArgumentResolver`
- `MaterialArgumentResolver`
- providers de suggestion com cache

## Configuracao Paper

`PaperPlatform.create(plugin)` carrega `plugin.getConfig()` automaticamente. Valores ausentes usam defaults seguros.

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

Carregamento manual:

```java
var config = PaperCommandEngineConfigLoader.load(getConfig());
var platform = PaperPlatform.create(this, config);
var engine = CommandEngine.create(platform);
```

## Anotacoes

| Anotacao | Alvo | Uso |
| --- | --- | --- |
| `@Command` | Classe | Define comando raiz, aliases, permissao e descricao. |
| `@Subcommand` | Metodo | Define caminho, permissao e descricao do subcomando. |
| `@Execute` | Metodo | Controla execucao async/sync. |
| `@Sender` | Parametro | Injeta `CommandSource` ou sender especifico da plataforma. |
| `@Arg` | Parametro | Define argumento Brigadier. |
| `@Flag` | Parametro | Define flag booleana longa e shorthand. |
| `@Optional` | Parametro | Define argumento opcional com valor padrao. |
| `@Greedy` | Parametro | Permite capturar texto com espacos. |
| `@Range`, `@Min`, `@Max` | Parametro | Valida argumentos numericos. |
| `@Suggestions` | Parametro/metodo | Vincula sugestoes por nome. |
| `@SuggestionProvider` | Metodo | Define metodo que fornece tab-complete. |

## Execution Model

Fluxo de execucao:

1. Brigadier parseia o comando.
2. O adapter gerado valida permissao e rate limit.
3. O adapter extrai `@Sender`, `@Arg` e `@Flag`.
4. O adapter chama o metodo do usuario diretamente.
5. Handlers `void` usam virtual threads por padrao.
6. Handlers que retornam `int` permanecem sincronizados, porque Brigadier precisa do resultado imediatamente.
7. Mensagens de erro sao enviadas via `CommandMessages` e `CommandScheduler`.

Exemplo de opt-out async:

```java
@Execute(async = false)
@Subcommand("reload")
public void reload(@Sender CommandSource source) {
    source.sendMessage("Reloaded.");
}
```

## Telemetry

Telemetry e opt-in atraves de `CommandTelemetry`.

```java
var telemetry = new LoggingCommandTelemetry(Logger.getLogger("CommandEngine"));

var engine = CommandEngine.builder()
        .brigadier(brigadierAdapter)
        .telemetry(telemetry)
        .build();
```

O runtime registra:

- duracao de execucao;
- falhas por `FailureReason`;
- modo async/sync;
- duracao e volume de sugestoes, quando aplicavel.

## Rate Limiting

O SPI publico e `CommandRateLimiter`:

```java
CommandRateLimiter limiter = (source, path) -> true;
```

O caminho configurado usa `CaffeineCommandRateLimiter`:

```java
var config = CommandEngineConfig.defaults();
var engine = CommandEngine.builder()
        .brigadier(brigadierAdapter)
        .config(config)
        .build();
```

Chave padrao:

```text
CommandSource.getName() + CommandPath
```

## Testes e Build

Rodar build completo no Windows:

```powershell
.\gradlew.bat spotlessCheck build --stacktrace
```

Aplicar formatacao e validar:

```powershell
.\gradlew.bat spotlessApply spotlessCheck build --stacktrace
```

Rodar somente testes:

```powershell
.\gradlew.bat test
```

Linux/macOS:

```bash
./gradlew spotlessCheck build --stacktrace
```

O CI executa:

```bash
./gradlew spotlessCheck build --stacktrace
```

O suite cobre:

- geracao do processor com compile-testing;
- comandos gerados em dispatcher Brigadier real;
- permissoes, aliases, greedy arguments, flags e opcionais;
- resolvers externos;
- mensagens configuraveis;
- rate limiting;
- Paper bridge, tab-complete, config e lifecycle;
- smoke tests de dispatch em volume.

## Arquitetura

Dependencias esperadas:

```text
commandengine-api
      ^
      |
commandengine-runtime
      ^
      |
commandengine-platform-paper

commandengine-processor --> commandengine-api
commandengine-test -----> commandengine-api/runtime/processor
```

Regras importantes:

- `api` nao deve depender de Paper, Caffeine ou implementacoes internas.
- `runtime` nao deve depender de `processor`.
- `platform-paper` pode depender de runtime, api, Paper e Caffeine.
- `processor` valida comandos e gera codigo, mas nao participa do runtime.
- `*.internal.*` nao deve ser usado como API publica.

## Documentacao

- [Quickstart](docs/QUICKSTART.md)
- [Architecture](docs/ARCHITECTURE.md)
- [Coding Standards](docs/CODING_STANDARDS.md)
- [Decision Log](docs/DECISION_LOG.md)
- [Production Checklist](docs/PRODUCTION_CHECKLIST.md)
- [Agent Guide](AGENTS.md)
- [Changelog](CHANGELOG.md)

## Roadmap

- Micrometer telemetry module.
- Graceful degradation de suggestions.
- Mais testes de integracao Paper.
- Smoke test manual em servidor Paper real.
- Publicacao de Javadocs.
- Gradle plugin para inspecao/validacao de command tree.
- Adapters Velocity, Fabric e Sponge.
- Benchmark suite.
- Preparacao para API estavel 1.0.

## Status de Producao

O projeto esta perto de uso interno controlado, mas ainda e alpha para consumo publico amplo.

Antes de usar em producao aberta:

- rode o plugin de exemplo em servidor Paper real;
- valide reload/disable e limpeza de comandos;
- valide aliases e tab-complete in-game;
- confirme que handlers async nao chamam APIs Paper thread-confined diretamente;
- rode [Production Checklist](docs/PRODUCTION_CHECKLIST.md);
- fixe uma versao/tag em vez de usar `main-SNAPSHOT`.

## Contribuicao

Antes de abrir PR:

1. Leia [AGENTS.md](AGENTS.md), [Architecture](docs/ARCHITECTURE.md) e
   [Coding Standards](docs/CODING_STANDARDS.md).
2. Mantenha mudancas pequenas e coesas.
3. Adicione testes quando mudar comportamento.
4. Rode:

```powershell
.\gradlew.bat spotlessApply spotlessCheck build --stacktrace
```

5. Verifique `git diff --stat` antes de commitar.

Padroes de codigo:

- Java 25.
- Javadocs de API publica em ingles.
- Records para dados imutaveis.
- Early returns.
- Sem reflection no runtime/hot path.
- Sem export acidental de `*.internal.*`.
- Spotless + Palantir Java Format.

## Licenca

MIT. Veja [LICENSE](LICENSE).
