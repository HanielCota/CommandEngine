# CommandEngine - Guia Para Agentes de IA

Este arquivo e o ponto de partida para qualquer agente de IA que va trabalhar neste repositorio. Leia isto antes de
editar codigo, revisar PRs, corrigir CI ou criar documentacao.

## Resumo do Projeto

CommandEngine e um framework de comandos compile-time para Java 25, focado primeiro em Paper/Purpur. O annotation
processor gera adapters Brigadier durante a compilacao, e o runtime executa chamadas diretas aos metodos do usuario.
Evite qualquer solucao que mova trabalho para reflection em runtime ou para descoberta dinamica no hot path.

Principios centrais:

- Compile-time first: validacao e geracao acontecem no `commandengine-processor`.
- Core agnostico de plataforma: `commandengine-api` e `commandengine-runtime` nao devem depender de Paper/Bukkit.
- Plataformas sao adapters: integracoes ficam em `commandengine-platform-*`.
- Pacotes `*.internal.*` nao sao API publica.
- Codigo gerado deve ser direto, previsivel, testavel e sem custo quando a feature nao for usada.

## Modulos

- `commandengine-api`: anotacoes, contratos publicos, SPIs, metadata, result types, scheduler, telemetry e rate limit.
- `commandengine-runtime`: facade `CommandEngine`, registry, executors, config, telemetry, cache e rate limiting.
- `commandengine-processor`: annotation processor, leitores de modelo e renderers de adapters/factories gerados.
- `commandengine-platform-paper`: bridge Paper/Purpur, command source, command map binding, resolvers e config loader.
- `commandengine-example-paper`: plugin minimo para smoke test manual.
- `commandengine-test`: harness, mocks, adapter local Brigadier e comandos de integracao.
- `docs`: arquitetura, quickstart, padroes de codigo, decision log e checklist de producao.

## Arquivos Que Normalmente Devem Ser Lidos

- `README.md`: visao geral, uso publico e comandos comuns.
- `docs/ARCHITECTURE.md`: fonte principal para decisoes arquiteturais.
- `docs/CODING_STANDARDS.md`: regras operacionais para agentes e contribuidores.
- `docs/QUICKSTART.md`: exemplos de uso e contrato esperado para usuarios.
- `.github/workflows/ci.yml`: comando real executado no CI.
- `build.gradle.kts` e `gradle/libs.versions.toml`: toolchain, plugins, formatting e dependencias.

## Comandos de Validacao

No Windows PowerShell:

```powershell
.\gradlew.bat spotlessCheck build --stacktrace
```

Para aplicar formatacao antes de validar:

```powershell
.\gradlew.bat spotlessApply spotlessCheck build --stacktrace
```

No Linux/macOS:

```bash
./gradlew spotlessCheck build --stacktrace
```

O CI roda exatamente:

```bash
./gradlew spotlessCheck build --stacktrace
```

Use `test` quando quiser uma verificacao mais curta, mas antes de pushar prefira o comando completo do CI.

## Regras de Edicao

- Preserve a separacao entre API, runtime, processor e platform.
- Nao adicione dependencias ao `commandengine-api` ou `commandengine-runtime` sem justificativa arquitetural.
- Nao exporte `*.internal.*` em `module-info.java`.
- Nao use reflection no runtime nem no hot path. Reflection em plataforma deve ficar isolada e bem justificada.
- Nao use `System.out.println`, `printStackTrace` ou falhas silenciosas.
- Javadocs de API publica devem estar em ingles.
- Siga Spotless/Palantir Java Format. Nunca confie so em compilacao local sem `spotlessCheck`.
- Ao mexer em lifecycle/cleanup, nao lance excecao dentro de `finally`; preserve falhas com `addSuppressed`.
- Evite padroes que confundem analise estatica, como `Map.compute(...)` quando o valor e tratado como nao-nulo logo
  depois.

## Padroes do Processor

O processor transforma classes anotadas com `@Command` em:

- `<Nome>CommandAdapter`
- `<Nome>CommandAdapterFactory`
- entradas de `META-INF/services` para `CommandAdapterFactory`

Ao alterar o processor:

- Adicione validacao em compile-time com mensagens claras.
- Teste regras novas em `commandengine-processor/src/test`.
- Prefira mudar renderers focados em vez de concentrar logica em um unico arquivo.
- Lembre que o codigo gerado tambem deve seguir as regras do projeto: early returns, mensagens configuraveis,
  scheduler para callbacks e sem reflection.

## Padroes do Runtime

- `CommandEngine` e a facade publica para registro, descoberta de factories e lifecycle.
- `DefaultCommandRegistry` mantem adapters por owner.
- `TelemetryCommandExecutor` envolve execucao e registra duracao/falhas.
- `VirtualThreadExecutor` executa handlers async e aplica timeout.
- `CaffeineCommandRateLimiter` e a implementacao padrao configuravel de rate limit.

Ao alterar runtime:

- Cubra regressao com testes em `commandengine-runtime/src/test`.
- Mantenha APIs publicas anotadas com `@NotNull`/`@Nullable`.
- Use `CommandMessages`, `CommandScheduler`, `CommandTelemetry` e `CommandRateLimiter` em vez de strings ou logica
  espalhada.

## Padroes da Plataforma Paper

- `PaperPlatform` monta registry, scheduler, executor, messages, rate limiter e resolvers nativos.
- `PaperBrigadierBinding` registra e remove comandos no `CommandMap`/dispatcher.
- `PaperBridgeCommand` adapta chamadas Bukkit para Brigadier.
- Resolvers nativos ficam em `commandengine-platform-paper/src/main/java/.../argument`.
- Suggestions com cache ficam em `.../suggestion`.

Ao alterar Paper:

- Nao vaze tipos Bukkit/Paper para `api` ou `runtime`.
- Use `PaperCommandScheduler` quando callbacks precisarem voltar para thread principal.
- Teste binding, config, arguments, suggestions e lifecycle em `commandengine-platform-paper/src/test`.

## CI, SonarCloud e Qualidade

O GitHub Actions valida formatacao e build. O SonarCloud avalia Quality Gate em cima do codigo novo.

Cuidados que ja causaram falha:

- `spotlessCheck` falha por quebras de linha diferentes do Palantir Java Format.
- Reliability Rating do SonarCloud pode cair por possivel NPE, `throw` em `finally`, resultado potencialmente nulo de
  `Map.compute`, ou cleanup que mascara excecoes anteriores.
- Quando o SonarCloud falhar sem annotations, consulte o check-run no GitHub e revise o diff novo procurando bugs
  reais de nullability, recursos, concorrencia e excecoes.

## Estado Local e Arquivos Suspeitos

Se existir uma pasta `org/` na raiz com classes Bukkit copiadas, trate como untracked suspeito. Ela nao pertence ao
layout normal do projeto e nao deve ser incluida em commits sem confirmacao explicita.

## Fluxo Recomendado Para Agentes

1. Rode `git status --short --branch`.
2. Leia este arquivo e os docs relevantes.
3. Entenda o modulo afetado antes de editar.
4. Faca mudancas pequenas e alinhadas ao padrao existente.
5. Rode `.\gradlew.bat spotlessApply spotlessCheck build --stacktrace`.
6. Confira `git diff --stat` e `git diff`.
7. So stageie arquivos relacionados ao pedido.
8. Se houver push, acompanhe GitHub Actions e SonarCloud ate concluir.

## Referencias Rapidas

- Release atual documentado: `v0.1.0-alpha.5`.
- Java baseline: 25.
- Gradle wrapper: 9.5.0.
- Paper API: `1.21.4-R0.1-SNAPSHOT`.
- Formater: Spotless + Palantir Java Format.
- Licenca: MIT.
