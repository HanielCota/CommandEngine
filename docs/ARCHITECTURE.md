# CommandEngine — Architecture Specification

> **Versão:** 0.1.0-alpha
> **Status:** Draft
> **Última revisão:** 2026-06-18
> **Autor:** @seuuser (comunitário)

---

## 1. Filosofia de Design (Non-Negotiable)

Estas regras são lei. Quebrar uma delas requer revisão arquitetural completa.

| # | Princípio                       | Justificativa                                                                                                            |
|---|---------------------------------|--------------------------------------------------------------------------------------------------------------------------|
| 1 | **Compile-Time First**          | Zero reflection no hot path. O processor APT gera adapters em `javac`. Runtime apenas executa chamadas diretas.          |
| 2 | **Zero Overhead Abstraction**   | Se o usuário não usar uma feature, ela não existe no bytecode gerado. Não pagamos pelo que não usamos.                   |
| 3 | **Plataforma = Adapter**        | O core (`api` + `runtime`) é 100% agnóstico. Paper, Velocity, Fabric, Sponge são módulos separados que implementam SPIs. |
| 4 | **Internal é Lei**              | Tudo em `*.internal.*` é detalhe de implementação. Não é API pública. Pode mudar entre minors.                           |
| 5 | **Production-Grade por Padrão** | Cache, telemetry, rate-limiting e graceful degradation não são "features futuras". São infraestrutura.                   |

---

## 2. Estrutura de Módulos (JPMS)

```
commandengine/
├── commandengine-api/              # API pública. Sagrada. Sem breaking changes sem major.
├── commandengine-processor/        # APT. Compile-time only. Não vai para runtime.
├── commandengine-runtime/          # Motor em produção. Leve, sem reflection.
├── commandengine-platform-paper/   # Adapter Paper/Purpur
├── commandengine-example-paper/    # Plugin Paper mínimo para smoke tests manuais
└── commandengine-test/             # Test utilities (MockCommandSource, etc.)
```

### Regras de Dependência

```
api ← runtime ← platform-xxx
      ↑
processor (depende apenas de api para ler anotações)
```

- **Proibido:** `platform-paper` depende de `platform-velocity`.
- **Proibido:** `runtime` depende de `processor`.
- **Permitido:** `api` expõe Brigadier como contrato público (`requires transitive brigadier`) e usa
  `org.jetbrains.annotations` como dependência estática.
- **Proibido:** `api` depender de bibliotecas de plataforma, cache, telemetry ou implementação.

---

## 3. Organização de Pacotes

### API (`com.hanielfialho.api`)

```
api/
├── annotation/          # @Command, @Subcommand, @Execute, @Arg, @Flag, @Sender, @Optional
├── command/             # CommandNode, CommandPath, CommandMetadata
├── source/              # CommandSource, PermissionHolder
├── argument/            # ArgumentType<T>, ArgumentResolver<T>, ResolvedArgument
├── executor/            # CommandExecutor, SyncExecutor, AsyncExecutor
├── result/              # CommandResult, Success, Failure, FailureReason
├── registry/            # CommandRegistry, RegistryBuilder
├── suggestion/          # SuggestionProvider, Suggestion
├── event/               # CommandEvent (SPI; pre/post execute events are not implemented yet)
├── message/             # CommandMessages
├── rate/                # CommandRateLimiter
├── scheduler/           # CommandScheduler
└── telemetry/           # CommandTelemetry (SPI)
```

### Processor (`com.hanielfialho.processor`)

```
processor/
├── CommandEngineProcessor.java
├── model/               # CommandModel, SubcommandModel, ParameterModel
├── reader/              # CommandModelReader
├── validation/          # SupportedCommandTypes, validation rules
├── resolver/            # SuggestionMethodResolver
├── generator/           # AdapterGenerator orchestrator + focused renderers
│   ├── AdapterImportsRenderer
│   ├── AdapterMemberRenderer
│   ├── AdapterTreeRenderer
│   ├── AdapterExecutionRenderer
│   ├── AdapterHelperRenderer
│   ├── AdapterMetadataRenderer
│   └── AdapterFactoryRenderer
└── writer/              # GeneratedSourceWriter, CommandAdapterServiceWriter
```

### Runtime (`com.hanielfialho.runtime`)

```
runtime/
├── internal/            # 🔒 NÃO É API PÚBLICA
│   ├── adapter/         # AbstractCommandAdapter, AdapterFactory
│   ├── dispatcher/      # CommandDispatcher, DispatchContext
│   ├── executor/        # ExecutorFactory, VirtualThreadExecutor, FallbackExecutor
│   ├── injection/       # ParameterInjector, ArgumentInjector, FlagInjector, SenderInjector
│   ├── registry/        # DefaultCommandRegistry
│   ├── suggestion/      # SuggestionEngine
│   ├── cache/           # CacheBackend (abstração), CaffeineCacheBackend
│   ├── rate/            # CaffeineCommandRateLimiter
│   └── telemetry/       # NoopTelemetry, AggregatedTelemetry
├── telemetry/           # LoggingCommandTelemetry and opt-in implementations
└── util/                # Preconditions, Strings
```

### Platform (`com.hanielfialho.platform.<nome>`)

```
platform.paper/
├── argument/            # PlayerArgumentResolver, WorldArgumentResolver, CachedArgumentResolver
├── binding/             # PaperBrigadierBinding
├── command/             # PaperBridgeCommand
├── config/              # PaperCommandEngineConfigLoader
├── listener/            # PluginDisableListener
├── source/              # PaperCommandSource
├── suggestion/          # PlayerSuggestionProvider, CachedSuggestionProvider
└── PaperPlatform.java
```

---

## 4. Sistema de Anotações

### @Command (TYPE)

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface Command {
    String name();                    // Nó raiz no Brigadier
    String[] aliases() default {};    // Aliases registrados
    String description() default "";  // Para /help nativo
    String permission() default "";   // Permissão base. Vazio = sem permissão.
}
```

### @Subcommand (METHOD)

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
public @interface Subcommand {
    String value();                   // Caminho: "reload" ou "player kick"
    String permission() default "";   // Herda de @Command se vazio
    String description() default "";
}
```

### @Execute (METHOD)

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
public @interface Execute {
    boolean async() default true;     // Void handlers use virtual threads by default
}
```

### @Arg (PARAMETER)

```java
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.CLASS)
public @interface Arg {
    String value();                   // Nome do argumento
    String description() default "";
    String suggests() default "";     // Método ou classe de sugestão
    int minLength() default 0;        // Validação
    int maxLength() default Integer.MAX_VALUE;
    double min() default Double.NEGATIVE_INFINITY;
    double max() default Double.POSITIVE_INFINITY;
}
```

### @Flag (PARAMETER)

```java
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.CLASS)
public @interface Flag {
    String value();                   // Nome longo: --force
    char shorthand() default '\0';   // Curto: -f
}
```

### @Sender (PARAMETER)

```java
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.CLASS)
public @interface Sender {}
```

### @Optional (PARAMETER)

```java
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.CLASS)
public @interface Optional {
    String defaultValue() default "";
}
```

### @RateLimit (METHOD — FUTURO)

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
public @interface RateLimit {
    int max();
    TimeUnit per();
}
```

---

## 5. Geração de Código (Processor)

### O que é gerado?

Para cada classe `@Command`, o processor gera:

1. **`<Nome>CommandAdapter.java`** — Implementa `CommandAdapter`. Contém a árvore Brigadier e as chamadas diretas ao
   handler.
2. **`<Nome>CommandAdapterFactory.java`** — Factory type-safe para instanciar o adapter.

### Regras de Geração

- **Chamadas diretas:** `instance.metodo(arg1, arg2)` — zero `Method#invoke`.
- **Injeção gerada:** Cada parâmetro (`@Sender`, `@Arg`, `@Flag`) vira uma linha de extração do `CommandContext`.
- **Async wrapper:** Handlers `void` usam `CompletableFuture` com `VirtualThreadExecutor` por padrão.
- **Sync opt-out:** `@Execute(async = false)` mantém um handler `void` síncrono.
- **Retorno `int`:** Handlers que retornam resultado Brigadier continuam síncronos.
- **Validação inline:** `@Arg(min = 1)` vira `if (value < 1) throw ...` no adapter.
- **Early return:** Adapters gerados evitam `else`, validando permissionamento, rate limit, sender e argumentos antes da
  chamada ao handler.

### Exemplo de Adapter Gerado

```java
@SuppressWarnings("all")
public final class GuildCommandAdapter implements CommandAdapter {

    private final GuildCommand instance;
    private final VirtualThreadExecutor asyncExecutor;

    public GuildCommandAdapter(GuildCommand instance, ExecutorProvider exec) {
        this.instance = instance;
        this.asyncExecutor = exec.async();
    }

    @Override
    public void register(BrigadierAdapter brigadier) {
        LiteralCommandNode<CommandSource> root = LiteralArgumentBuilder
            .<CommandSource>literal("guild")
            .requires(src -> src.hasPermission("guild.use"))
            .build();

        // guild create <name>
        LiteralCommandNode<CommandSource> create = LiteralArgumentBuilder
            .<CommandSource>literal("create")
            .executes(ctx -> {
                if (!(ctx.getSource().getHandle() instanceof Player player)) {
                    ctx.getSource().sendMessage("This command cannot be executed by this sender.");
                    return 0;
                }
                String name = StringArgumentType.getString(ctx, "name");
                instance.create(player, name);
                return 1;
            })
            .build();

        create.addChild(RequiredArgumentBuilder
            .<CommandSource, String>argument("name", StringArgumentType.string())
            .build());

        root.addChild(create);
        brigadier.getDispatcher().getRoot().addChild(root);
    }
}
```

---

## 6. Runtime: Execução e Infraestrutura

### Dispatcher

- Recebe `CommandContext` do Brigadier.
- Extrai `CommandSource`.
- Roteia para o adapter correto (hash map O(1) por path).
- Telemetry é registrada pelo `TelemetryCommandExecutor` ao redor da execução.

### Async Executor

```java
public class VirtualThreadExecutor {
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public <T> CompletableFuture<T> submit(Supplier<T> task) {
        return CompletableFuture.supplyAsync(task, executor);
    }
}
```

**Fallback:** Se a JVM não suportar virtual threads, `Executors.newCachedThreadPool()` é usado em vez de
`Executors.newVirtualThreadPerTaskExecutor()`.

### Cache (Caffeine)

- **Localização:** `runtime` e plataformas que precisam de cache local.
- **Uso:** `ArgumentResolver` e `SuggestionProvider` podem usar cache interno.
- **Config:** TTL e max-size via `commandengine.yml`.
- **Implementação:** `CaffeineCacheBackend`, sempre com TTL e tamanho máximo definidos.

### Rate Limiting

- SPI: `CommandRateLimiter`.
- Implementação padrão opt-in: `CaffeineCommandRateLimiter`.
- Chave: `CommandSource.name()` + `CommandPath`.
- Configuração: `CommandEngineConfig` define janela, máximo de execuções por janela e tamanho máximo do cache.
- Integração: o adapter gerado bloqueia antes da execução do handler e envia `CommandMessages.rateLimited()`.

### Telemetry

- SPI: `CommandTelemetry`.
- Implementação padrão: `CommandTelemetry.NOOP`.
- Integração: `TelemetryCommandExecutor` registra duração, falha e modo async/sync por `CommandPath`.
- Implementação opt-in atual: `LoggingCommandTelemetry`.
- Implementação opt-in futura: `MicrometerTelemetry` (requer `micrometer-core`).

### Scheduler e mensagens

- `CommandScheduler` centraliza callbacks que precisam voltar para a thread principal da plataforma.
- `PaperCommandScheduler` usa a scheduler do Bukkit quando o callback sai de uma virtual thread.
- `CommandMessages` centraliza mensagens de erro internas, sender inválido, sintaxe inválida, permissão e rate limit.
- `CommandEngineConfig` agrupa mensagens, timeout async e rate limit básico.
- `CommandEngine.Builder` permite configurar `scheduler`, `messages`, `telemetry`, `rateLimiter`, `config` e
  `asyncTimeout`.

---

## 7. Plataformas

### Paper

- Usa `PaperBrigadierBinding` para registrar no `CommandDispatcher` nativo do Paper.
- `PaperCommandSource` adapta `CommandSender` (Player, Console, etc.) para `CommandSource`.
- Resolvers nativos: `Player`, `World`, `Location`, `Material`.
- `PaperCommandEngineConfigLoader` carrega `CommandEngineConfig` de `plugin.getConfig()`.
- `PaperPlatform.create(plugin)` usa config externa quando disponível e mantém defaults seguros.

### Paper Example

- `commandengine-example-paper` compila um plugin mínimo com `plugin.yml`, `config.yml` e comando real processado pelo
  annotation processor.
- O comando `/cexample ping` valida registro, permissão, bridge Paper e mensagem síncrona.
- O comando `/cexample echo <message>` valida argumento greedy e execução async.

---

## 8. Segurança e Produção

### Input Sanitization

- `@Arg(maxLength = N)` enforce no adapter gerado.
- `@Arg` `String` com `§` (formatting codes) é stripado por padrão em `PaperCommandSource`.
- `@Arg` regex validation (futuro).

### Memory Leaks / Reloads

- `CommandAdapter` tem `unregister()`.
- `DefaultCommandRegistry` tem `unregisterAll(Object owner)`.
- `PaperEventBridge` escuta `PluginDisableEvent` para auto-unregister.

---

## 9. Build e Dependências

### Gradle Version Catalog (`libs.versions.toml`)

```toml
[versions]
java = "25"
caffeine = "3.1.8"
minecraft = "1.21.4"
paper = "1.21.4-R0.1-SNAPSHOT"

[libraries]
caffeine = { module = "com.github.ben-manes.caffeine:caffeine", version.ref = "caffeine" }
paper-api = { module = "io.papermc.paper:paper-api", version.ref = "paper" }

[bundles]
platform-paper = ["paper-api", "caffeine"]
```

### JPMS

- Cada módulo tem `module-info.java`.
- `api` exports apenas pacotes públicos.
- `runtime` NÃO exports `*.internal.*`.
- Plataformas exportam apenas entrypoints e resolvers.

---

## 10. Testabilidade

### Módulo `commandengine-test`

```java
// MockCommandSource.java
public class MockCommandSource implements CommandSource {
    private final String name;
    private final Set<String> permissions = new HashSet<>();
    private final List<Component> messages = new ArrayList<>();

    public boolean hasPermission(String perm) { return permissions.contains(perm); }
    public void sendMessage(Component msg) { messages.add(msg); }
    public List<Component> getMessages() { return messages; }
}

// TestEngine.java
public class TestEngine {
    public static CommandEngine create() {
        return CommandEngine.builder()
            .executor(new SyncExecutor()) // Sem threads para testes
            .registry(new DefaultCommandRegistry())
            .build();
    }
}
```

### Testes de Integração

- Processor: `compile-testing` (Google) para testar geração de código.
- Runtime: JUnit 5 + AssertJ.
- Platform: MockBukkit (Paper) ou Testcontainers (se necessário).

---

## 11. Roadmap

### Fase 0 — Fundação (Semanas 1-2)

- [ ] Setup Gradle multimódulo com JPMS
- [ ] API finalizada: anotações + interfaces
- [ ] Processor básico: lê `@Command` + `@Subcommand`, gera adapter "hello world"
- [ ] Teste: comando literal sem args funcionando no Paper

### Fase 1 — Brigadier Nativo (Semanas 3-4)

- [ ] `TreeGenerator`: gerar `LiteralArgumentBuilder` e `RequiredArgumentBuilder`
- [ ] Integração com `CommandDispatcher` do Brigadier
- [ ] Suporte a aliases
- [ ] Permissões nativas no `requires()`

### Fase 2 — Injeção de Parâmetros (Semanas 5-6)

- [ ] `@Sender`, `@Arg`, `@Flag`
- [ ] SPI para `ArgumentTypeResolver`
- [ ] Resolvers nativos: `String`, `Integer`, `Double`, `Boolean`, `Player` (Paper)
- [ ] Validação em compile-time para tipos nativos; tipos externos exigem `ArgumentTypeResolver` registrado

### Fase 3 — Async & Performance (Semana 7)

- [x] Handlers `void` async por padrão com Virtual Threads
- [ ] Benchmark JMH: reflection vs generated adapter
- [ ] Otimização: evitar boxing desnecessário

### Fase 4 — Multiplataforma (Semanas 8-10)

- [ ] Adapter Velocity
- [ ] Adapter Fabric
- [ ] Adapter Sponge (se vigente)
- [ ] Standalone (aplicações não-Minecraft com Brigadier)

### Fase 5 — Infraestrutura de Produção (Semanas 11-12)

- [x] Caffeine integration em runtime e plataformas
- [x] Telemetry SPI integrada ao executor
- [x] Logging telemetry opt-in
- [x] Platform scheduler para callbacks Paper
- [x] Mensagens configuráveis
- [x] JaCoCo reports no build
- [x] Rate limiting básico com Caffeine
- [x] Configuração externa Paper via YAML nativo
- [x] GitHub Actions CI com JDK 25
- [x] Checklist de produção documentado
- [ ] Micrometer telemetry module
- [ ] Graceful degradation de suggestions
- [ ] Memory leak protection (unregister)

### Fase 6 — DX e Comunidade

- [ ] Gradle plugin: `printCommandTree`, `validateCommands`
- [ ] Quick-start template (GitHub Template)
- [ ] Javadoc publicado
- [ ] Release 1.0.0

---

## 12. Convenções de Código

### Nomenclatura

| Tipo              | Padrão                       | Exemplo                                           |
|-------------------|------------------------------|---------------------------------------------------|
| Anotação          | Substantivo/Verbo            | `@Command`, `@Execute`                            |
| Interface pública | Substantivo descritivo       | `CommandSource`, `ArgumentResolver`               |
| Classe interna    | Implementação concreta       | `DefaultCommandRegistry`, `VirtualThreadExecutor` |
| Adapter gerado    | `NomeOriginalCommandAdapter` | `GuildCommandAdapter`                             |
| Exceção           | `XException`                 | `ProcessingException`, `DispatchException`        |

### Estilo

- **Java 25** como baseline.
- **Preview features:** Não usar em código de produção (exceto `ScopedValue` se estável).
- **Records:** Usar para DTOs imutáveis (`CommandPath`, `CommandMetadata`).
- **Sealed classes:** Usar para hierarquias fechadas (`CommandResult`, `FailureReason`).
- **Nullability:** `@Nullable` / `@NotNull` do JetBrains em toda API pública.
- **Final:** Classes e métodos são `final` por padrão. Abrir apenas quando necessário.

---

## 13. Licença e Governança

- **Licença:** MIT
- **Monorepo:** Sim, com Gradle Version Catalogs
- **CI/CD:** GitHub Actions (build multi-JDK, testes, JMH)
- **Documentação:** MkDocs Material
- **Versionamento:** SemVer desde `0.1.0`
- **Código de Conduta:** Contributor Covenant

---

*Este documento é a fonte da verdade. Alterações arquiteturais devem ser propostas via PR com justificativa e revisão.*
