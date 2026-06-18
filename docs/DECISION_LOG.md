# CommandEngine — Architecture Decision Log (ADL)

> **Formato:** ADR (Architecture Decision Records)
> **Número sequencial:** ADR-001 em diante

---

## ADR-001: Compile-Time Code Generation via Annotation Processor

**Status:** Accepted
**Data:** 2026-06-18
**Contexto:** Frameworks de comando existentes usam reflection em runtime para descobrir métodos e anotações. Isso causa
overhead em servidores grandes e impede GraalVM native-image.

**Decisão:** Usar Java Annotation Processor (APT) para gerar adapters em compile-time. Runtime executa apenas chamadas
diretas.

**Consequências:**

- ✅ Zero reflection no hot path
- ✅ Compatível com GraalVM native-image
- ✅ Erros de configuração em compile-time (fail fast)
- ❌ Build mais lento (processor roda a cada compilação)
- ❌ Não é possível registrar comandos dinamicamente em runtime (sem recompilar)

---

## ADR-002: JPMS (Java Platform Module System) desde o início

**Status:** Accepted
**Data:** 2026-06-18
**Contexto:** Sem JPMS, qualquer classe pública é acessível. Encapsulamento depende de convenção (nomes `internal`).

**Decisão:** Todo módulo tem `module-info.java`. Pacotes `internal` não são exportados.

**Consequências:**

- ✅ Encapsulamento real
- ✅ Dependências explícitas
- ✅ JLink-friendly (futuro)
- ❌ Curva de aprendizado para contribuidores
- ❌ Algumas libs legacy podem não ter module-info (requer `requires transitive` ou `automatic module`)

---

## ADR-003: Caffeine como backend de cache padrao

**Status:** Superseded
**Data:** 2026-06-18
**Contexto:** Cache e necessario para servidores grandes, e o fallback anterior baseado em `ConcurrentHashMap` podia
crescer sem limite quando usado com chaves controladas por entrada externa.

**Decisao:** Caffeine e dependencia `implementation` dos modulos que usam cache. O runtime mantem a abstracao
`CacheBackend`, mas a implementacao interna padrao e `CaffeineCacheBackend`, com TTL e tamanho maximo configuraveis.

**Consequências:**

- Cache bounded por padrao
- Uma implementacao de cache para manter
- Plataformas continuam sem expor Caffeine na API publica
- Runtime passa a ter dependencia de implementacao em Caffeine

---

## ADR-004: Virtual Threads como baseline para async

**Status:** Accepted
**Data:** 2026-06-18
**Contexto:** Java 25 tem Virtual Threads (JEP 444). Servidores Minecraft ainda usam thread pools fixos para async.

**Decisão:** Handlers `void` executam em virtual threads por padrão pelo `CommandExecutor`. `@Execute(async = false)`
mantem um handler `void` sincrono quando a plataforma exigir isso. Handlers que retornam `int` continuam sincronos
porque o Brigadier precisa do resultado imediatamente.

**Consequências:**

- ✅ Milhares de comandos async simultâneos sem starvation
- ✅ Simplifica código (parece síncrono, mas não é)
- ❌ Requer Java 25+ (baseline definida)
- ❌ Cuidado com carrier thread pinning (synchronized + I/O)

---

## ADR-005: Monorepo com Gradle Version Catalogs

**Status:** Accepted
**Data:** 2026-06-18
**Contexto:** Multiplataforma implica múltiplos módulos. Versões devem ser consistentes.

**Decisão:** Monorepo Gradle com `gradle/libs.versions.toml`. Cada plataforma é submódulo independente.

**Consequências:**

- ✅ Atomic commits que tocam API + Processor + Runtime
- ✅ CI/CD simplificado
- ✅ Versionamento centralizado
- ❌ Repositório maior (mas apenas código fonte, não bins)

---

## ADR-006: Telemetry como SPI, não core feature

**Status:** Accepted
**Data:** 2026-06-18
**Contexto:** Observabilidade é crucial em produção, mas adicionar Micrometer/Prometheus ao core aumenta deps.

**Decisão:** `CommandTelemetry` é interface no `api`. `CommandTelemetry.NOOP` é padrão. `TelemetryCommandExecutor`
integra a SPI ao runtime. Implementações (Micrometer, Prometheus) são módulos separados ou parte das plataformas.

**Consequências:**

- ✅ Core sem dependências de observabilidade
- ✅ Usuário opta pelo backend
- ❌ Descoberta de implementação via ServiceLoader (custo mínimo)

---

## ADR-007: Baseline Java 25

**Status:** Accepted
**Data:** 2026-06-18
**Contexto:** Java 25 é a baseline moderna do projeto, com Virtual Threads, Sealed Classes e Pattern Matching for switch
estabilizados para o estilo atual do framework.

**Decisão:** Java 25 é baseline. Não suportamos Java 21 ou anterior.

**Consequências:**

- ✅ Usa Virtual Threads nativamente
- ✅ Pattern matching em switch para resolvers
- ✅ Sealed classes para `CommandResult`
- ❌ Exclui servidores legacy (aceitável para projeto novo em 2026)

---

## ADR-008: Brigadier como contrato público da API

**Status:** Accepted
**Data:** 2026-06-18
**Contexto:** `ArgumentTypeResolver`, `BrigadierAdapter` e os adapters gerados expõem tipos Brigadier. Tratar Brigadier
como detalhe interno faria consumidores modulares compilarem contra uma API incompleta.

**Decisão:** Brigadier é dependência pública explícita do `commandengine-api` (`requires transitive brigadier` e
`api(libs.brigadier)`). Bibliotecas de plataforma, cache e telemetry continuam proibidas no `api`.

**Consequências:**

- ✅ Contrato público honesto para consumidores JPMS e Gradle
- ✅ Processor e runtime compartilham a mesma árvore de comando
- ❌ Trocar Brigadier futuramente exigirá major version

---

## ADR-009: Remoção de nós Brigadier via reflection isolada

**Status:** Accepted
**Data:** 2026-06-18
**Contexto:** Brigadier permite adicionar filhos à raiz, mas não oferece API pública para remover comandos registrados.
Paper precisa limpar comandos em reload/disable para evitar vazamento de comandos antigos.

**Decisão:** A mutação reflectiva dos mapas internos da raiz Brigadier é permitida apenas em adapters de plataforma e
deve ficar isolada em classe interna dedicada. No Paper, essa exceção vive em `BrigadierRootMutator`.

**Consequências:**

- ✅ Suporta unregister/reload sem expor reflection no hot path
- ✅ A exceção fica auditável e testável
- ❌ Pode quebrar se Brigadier mudar nomes internos de campos

---

## ADR-010: Rate limiting como SPI com implementação Caffeine

**Status:** Accepted
**Data:** 2026-06-18
**Contexto:** Comandos podem ser disparados por entrada externa e precisam de proteção básica contra spam sem acoplar a
API pública a uma biblioteca de cache.

**Decisão:** Expor `CommandRateLimiter` no `api`. O runtime oferece `CaffeineCommandRateLimiter` como implementação
interna configurável por `CommandEngineConfig`. O adapter gerado consulta o limiter antes de executar o handler.

**Consequências:**

- ✅ Proteção bounded por fonte + caminho de comando
- ✅ API pública pequena e testável
- ✅ Sem dependência de Caffeine no `api`
- ❌ Rate limit distribuído ainda exige implementação externa

---

## ADR-011: Generator dividido em renderers focados

**Status:** Accepted
**Data:** 2026-06-18
**Contexto:** `AdapterGenerator` acumulava imports, membros, árvore Brigadier, execução e helpers, dificultando revisão e
evolução.

**Decisão:** Manter `AdapterGenerator` como orquestrador e mover responsabilidades para renderers focados:
`AdapterImportsRenderer`, `AdapterMemberRenderer`, `AdapterTreeRenderer`, `AdapterExecutionRenderer`,
`AdapterHelperRenderer`, `AdapterMetadataRenderer` e `AdapterFactoryRenderer`.

**Consequências:**

- ✅ Menor acoplamento no processor
- ✅ Mudanças de execução, árvore e metadados ficam isoladas
- ✅ Facilita testes e revisão futura do código gerado
- ❌ Ainda é geração por `StringBuilder`; JavaPoet continua uma opção futura para blocos mais complexos

---

## ADR-012: Configuração Paper carregada por Bukkit FileConfiguration

**Status:** Accepted
**Data:** 2026-06-18
**Contexto:** Plugins Paper já possuem `config.yml` e `Plugin#getConfig()`. Introduzir outra biblioteca de YAML criaria
mais uma dependência e mais um formato operacional.

**Decisão:** `commandengine-platform-paper` expõe `PaperCommandEngineConfigLoader`, que carrega `CommandEngineConfig` a
partir de `FileConfiguration`. `PaperPlatform.create(plugin)` usa esse loader por padrão.

**Consequências:**

- ✅ Sem dependência YAML extra
- ✅ Integra naturalmente com plugins Paper
- ✅ Defaults continuam centralizados em `CommandEngineConfig`
- ❌ Configuração externa fora do Paper ainda precisa de loader próprio

---

## ADR-013: CI obrigatório com JDK 25

**Status:** Accepted
**Data:** 2026-06-18
**Contexto:** O projeto depende de Java 25, annotation processing, JPMS e formatação automática. Regressões nessas áreas
precisam ser detectadas antes de merge.

**Decisão:** Usar GitHub Actions para rodar `./gradlew spotlessCheck build --stacktrace` com JDK 25 em pushes e pull
requests.

**Consequências:**

- ✅ Build e testes deixam de depender apenas da máquina local
- ✅ Formatação vira gate de PR
- ✅ Geração de código é validada em CI
- ❌ O pipeline depende de disponibilidade de JDK 25 no provedor configurado

---

*Novas decisões devem ser adicionadas ao final deste arquivo com número sequencial e data.*
