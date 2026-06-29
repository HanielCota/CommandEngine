# CommandEngine — Quickstart

Este guia mostra o caminho mínimo para usar o CommandEngine em Java 25.

## Requisitos

- JDK 25.
- Gradle 9.6.1.
- Annotation processor habilitado no módulo que contém os comandos.
- Build com Spotless: `./gradlew build` executa `spotlessCheck` antes de compilar.

## Dependências Gradle

Via JitPack, adicione o repositório:

```kotlin
dependencyResolutionManagement {
  repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://jitpack.io")
  }
}
```

E consuma os módulos publicados:

```kotlin
val commandEngineVersion = "v0.1.0-alpha.6"

dependencies {
  implementation("com.github.HanielCota.CommandEngine:commandengine-api:$commandEngineVersion")
  implementation("com.github.HanielCota.CommandEngine:commandengine-runtime:$commandEngineVersion")
  annotationProcessor("com.github.HanielCota.CommandEngine:commandengine-processor:$commandEngineVersion")
}
```

Para Paper, adicione também:

```kotlin
implementation("com.github.HanielCota.CommandEngine:commandengine-platform-paper:$commandEngineVersion")
```

Como o repositório é público, o consumidor não precisa configurar credenciais no JitPack.

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

## Primeiro Comando

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
        description = "Sistema de warps")
public final class WarpCommand {

    @Subcommand("teleport")
    public void teleport(@Sender CommandSource source, @Arg("name") @Suggestions("warpNames") String warpName) {
        source.sendMessage("Teleportando para " + warpName);
    }

    @Subcommand(value = "create", permission = "warp.admin")
    public void create(@Sender CommandSource source, @Arg("name") String name, @Arg("cost") @Range(min = 0, max = 10_000) int cost) {
        source.sendMessage("Warp criada: " + name + " por " + cost);
    }

    @Subcommand(value = "broadcast", permission = "warp.admin")
    public void broadcast(@Arg("message") @Greedy String message) {
        // Envie a mensagem pelo serviço do plugin.
    }

    @SuggestionProvider("warpNames")
    public List<String> warpNames() {
        return List.of("spawn", "shop", "arena");
    }
}
```

O processor gera um adapter e uma factory em tempo de compilação. O runtime localiza a factory com `ServiceLoader`,
então não há reflection no hot path.

### JPMS / `module-info.java`

Se o módulo dos seus comandos declarar um `module-info.java`, o adapter factory gerado deve ser exposto via
`provides`:

```java
module com.example.myplugin {
    requires com.hanielfialho.api;
    requires com.hanielfialho.runtime;

    provides com.hanielfialho.api.command.CommandAdapterFactory
        with com.example.WarpCommandCommandAdapterFactory;
}
```

Sem essa declaração, `CommandEngine.register(...)` não encontrará o adapter factory em runtime.

## Registro no Runtime

```java
var engine = CommandEngine.builder()
        .brigadier(brigadierAdapter)
        .owner(pluginOrApplication)
        .build();

engine.register(new WarpCommand());
```

Com configuração centralizada e rate limit básico:

```java
var config = CommandEngineConfig.defaults();
var engine = CommandEngine.builder()
        .brigadier(brigadierAdapter)
        .config(config)
        .telemetry(new LoggingCommandTelemetry(Logger.getLogger("CommandEngine")))
        .build();
```

## Registro no Paper

```java
public final class ExamplePlugin extends JavaPlugin {

    private CommandEngine engine;

    @Override
    public void onEnable() {
        engine = CommandEngine.create(PaperPlatform.create(this));
        engine.register(new WarpCommand());
    }

    @Override
    public void onDisable() {
        if (engine != null) {
            engine.close();
        }
    }
}
```

O `PaperPlatform` registra resolvers nativos para `Player`, `World`, `Location` e `Material`, usa virtual threads no
executor padrão e limpa comandos registrados quando o plugin é desabilitado.

### Configuração Paper

`PaperPlatform.create(plugin)` carrega automaticamente `plugin.getConfig()` usando as chaves abaixo. Valores ausentes
usam defaults seguros.

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

Também é possível carregar manualmente:

```java
var config = PaperCommandEngineConfigLoader.load(getConfig());
var platform = PaperPlatform.create(this, config);
var engine = CommandEngine.create(platform);
```

## Plugin Paper de Exemplo

O módulo `commandengine-example-paper` gera um plugin mínimo. Depois do build, o JAR fica em:

```text
commandengine-example-paper/build/libs/commandengine-example-paper-0.1.0-SNAPSHOT.jar
```

Comandos do exemplo:

- `/cexample ping`
- `/cexample echo <message>`

## Recursos Suportados

- Aliases de raiz com a mesma árvore Brigadier do comando principal.
- Permissões no comando raiz e por subcomando.
- `@Sender` para injeção da origem.
- `@Arg` para argumentos Brigadier.
- `@Flag` com forma longa e shorthand.
- `@Greedy` para textos com espaços.
- `@Range`, `@Min` e `@Max` para validação numérica nativa.
- `@Optional` para argumentos com valor padrão. Tipos customizados precisam declarar `defaultValue` e usar um
  `ArgumentTypeResolver` que implemente `resolveDefault` e declare `supportsDefault() = true`.
- `@Suggestions` e `@SuggestionProvider` para tab-complete. Providers são síncronos por padrão; use
  `@SuggestionProvider(async = true)` apenas para providers thread-safe ou cacheados.
- Handlers `void` executam de forma síncrona por padrão.
- Use `@Execute(async = true)` quando um handler `void` precisar rodar no executor baseado em virtual threads.
- Configure `CommandEngine.builder().messages(...)`, `.telemetry(...)`, `.scheduler(...)`, `.rateLimiter(...)`,
  `.suggestionExecutor(...)`, `.config(...)` e `.asyncTimeout(...)` quando a plataforma precisar de comportamento
  customizado.
- `CommandEngineConfig.defaults()` habilita a configuração centralizada de mensagens, timeout async e rate limit com
  Caffeine.

## Testes

Use `com.hanielfialho.test.engine.TestEngine.harness()` para validar adapters gerados com um `CommandDispatcher`
real:

```java
var harness = com.hanielfialho.test.engine.TestEngine.harness();
var source = new com.hanielfialho.test.source.MockCommandSource("tester");
// Deprecated aliases also exist under com.hanielfialho.test.* for compatibility.
source.addPermission("warp.use");

harness.engine().register(new WarpCommand());
harness.dispatcher().execute("w teleport spawn", source);
```

O módulo `commandengine-test` também contém comandos de integração em `com.hanielfialho.test.command` e exemplos
compiláveis em `com.hanielfialho.test.examples`.
