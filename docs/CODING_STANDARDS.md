# CommandEngine — Coding Standards & Agent Instructions

> **Para:** Code agents, contribuidores, revisores
> **Versão:** 0.1.0
> **Prioridade:** Obedecer a `ARCHITECTURE.md` é obrigatório. Este documento é operacional.

---

## 1. Regras de Ouro (Nunca Quebrar)

1. **NUNCA** use reflection no `runtime` ou no hot path de execução. Reflection em `platform-xxx` exige ADR e deve ficar
   isolada em uma classe interna pequena.
2. **NUNCA** adicione dependências ao `api` ou `runtime` core sem ADR. Brigadier é a exceção aceita porque faz parte do
   contrato público; dependências de plataforma vão nos `platform-xxx`.
3. **NUNCA** exporte pacotes `*.internal.*` no `module-info.java`.
4. **SEMPRE** gere código via processor em vez de fazer runtime discovery.
5. **SEMPRE** use `CommandSource` em vez de tipos de plataforma (Player, CommandSender) no core.

---

## 2. Estrutura de Arquivos

### Novo módulo

```
commandengine-novo-modulo/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/hanielfialho/novo/
│   │   │       ├── internal/          # 🔒 Implementação
│   │   │       └── [Pacotes públicos] # 📢 API
│   │   └── resources/
│   └── test/
│       └── java/
└── build.gradle.kts
```

### Novo arquivo

1. **Package declaration:** Deve corresponder ao diretório.
2. **License header:** MIT header no topo.
3. **Imports:** `java.*` primeiro, `javax.*` segundo, bibliotecas terceiro, `com.hanielfialho.*` por último. Sem
   wildcard imports (`*`).
4. **Classe pública:** Documente com Javadoc. Explique o **por quê**, não o **o quê**.
5. **Javadocs:** Escreva Javadocs em inglês.
6. **Records:** Use para dados imutáveis. Não use Lombok.

---

## 3. Padrões de Implementação

### Fluxo de controle

- Prefira early return para validações, guards e caminhos de falha.
- Não use `else` em código Java novo ou gerado; depois de um `return`, `throw`, `continue` ou `break`, siga com o fluxo
  principal sem abrir outro bloco.
- Mantenha loops com no máximo um `break` ou `continue`; extraia métodos quando a condição ficar longa.
- Valide argumentos externos e objetos injetados no início do método.

### Processor (APT)

```java
// ✅ Correto: Validação em compile-time, erro claro
if (paramType.getKind() == TypeKind.VOID) {
    messager.printMessage(
        Diagnostic.Kind.ERROR,
        "@Arg parameters cannot be void",
        paramElement
    );
    return; // Não gere código inválido
}

// ✅ Correto: Use um renderer estruturado (JavaPoet é preferível quando o bloco for complexo)
MethodSpec method = MethodSpec.methodBuilder("register")
    .addAnnotation(Override.class)
    .addModifiers(Modifier.PUBLIC)
    .addParameter(BrigadierAdapter.class, "brigadier")
    .returns(void.class)
    .build();

// ❌ Errado: Concatenar strings soltas dentro da lógica de validação/modelo
String code = "public void register(" + type + " brigadier) { ... }";
```

### Runtime

```java
// ✅ Correto: valida o sender antes da chamada direta
public int execute(CommandContext<CommandSource> ctx) {
    if (!(ctx.getSource().getHandle() instanceof Player player)) {
        ctx.getSource().sendMessage("This command cannot be executed by this sender.");
        return 0;
    }
    int amount = IntegerArgumentType.getInteger(ctx, "amount");
    instance.pay(player, amount); // Chamada direta!
    return 1;
}

// ❌ Errado: Reflection ou boxing
Object player = method.invoke(null, ctx.getSource()); // NUNCA
Integer amount = (Integer) ctx.getArgument("amount"); // Evite wrapper
```

### Platform

```java
// ✅ Correto: Adapter delega, não duplica lógica
public class PaperCommandSource implements CommandSource {
    private final CommandSender handle;

    @Override
    public boolean hasPermission(String permission) {
        return handle.hasPermission(permission); // Delegação
    }

    @Override
    public Object getHandle() {
        return handle; // Para casos específicos da plataforma
    }
}

// ❌ Errado: Lógica de negócio na plataforma
public void pay(Player p, int amount) { // NÃO! Isso é do usuário
    economy.withdraw(p, amount);
}
```

---

## 4. Cache e Performance

### Quando usar Caffeine

- **Deve:** `ArgumentResolver` com lookup caro (Player por nome).
- **Deve:** `SuggestionProvider` com listas grandes (players online). Providers são síncronos por padrão; use
  `@SuggestionProvider(async = true)` só quando o provider for thread-safe ou cacheado.
- **Pode:** `PermissionHolder` cache se verificação for complexa.
- **Não deve:** Resultado de execução de comando (side-effects).
- **Não deve:** `CommandSource` ou objetos mutáveis.

### Configuração padrão

```java
Cache<String, Player> cache = Caffeine.newBuilder()
    .expireAfterWrite(2, TimeUnit.SECONDS)
    .maximumSize(500)
    .recordStats() // Se telemetry habilitada
    .build();
```

---

## 5. Async e Virtual Threads

### Regras

1. Handlers `void` executam de forma síncrona por padrão; use `@Execute(async = true)` apenas para opt-in async.
2. **Nunca** faça I/O bloqueante (SQL, HTTP, sleep) na thread principal.
3. **Sempre** capture `CommandSource` e contexto **antes** de enviar para async.
4. **Sempre** propague exceções de volta ao dispatcher para tratamento.
5. Use `CommandScheduler` para callbacks que precisam tocar APIs de plataforma na thread principal.
6. Use `CommandMessages` para mensagens configuráveis; não espalhe strings user-facing pelo código gerado.

### Template de Adapter Async

```java
// Gerado pelo processor quando o handler usa @Execute(async = true)
executor.executeAsync(source, path, () -> instance.pay(player, amount))
    .thenAccept(result -> handleResult(source, result, scheduler))
    .exceptionally(throwable -> handleAsyncFailure(source, throwable, scheduler, messages));
```

---

## 6. Testes

### Obrigatórios

- **Unit:** Toda classe em `runtime` deve ter teste.
- **Processor:** Toda regra de validação deve ter teste com `compile-testing`.
- **Integration:** Cada `platform-xxx` deve ter pelo menos 1 teste de integração.

### Proibido

- Testes que dependem de servidor real (use MockBukkit/MockVelocity).
- `Thread.sleep()` em testes (use `Awaitility` ou `CompletableFuture` timeouts).
- Testes sem assertions descritivos.

---

## 7. Commits e PRs

### Mensagens de commit (Conventional Commits)

```
feat(processor): add support for @Flag annotation
fix(runtime): prevent NPE when CommandSource is null
docs(api): clarify @Arg description behavior
perf(platform-paper): cache player lookup in PlayerArgumentResolver
test: add integration test for async command execution
```

### PR Checklist

- [ ] `ARCHITECTURE.md` atualizado se houver mudança estrutural.
- [ ] `module-info.java` verificado (nenhum export acidental de internal).
- [ ] Testes passam (`./gradlew test`).
- [ ] Javadoc adicionado para API pública.
- [ ] Sem breaking changes em `api` (ou marcado como `@Deprecated` com `@since`).

---

## 8. Anti-Patterns (Proibidos)

| Anti-Pattern                       | Por que é proibido                       | Solução                                                      |
|------------------------------------|------------------------------------------|--------------------------------------------------------------|
| `Class.forName()` no runtime       | Quebra GraalVM native-image. Reflection. | Processor gera referência direta.                            |
| `e.printStackTrace()`              | Não controlável. Sem contexto.           | Use `Logger` ou `Telemetry`.                                 |
| `System.out.println()`             | Mesmo problema.                          | Use `Logger` ou `Telemetry`.                                 |
| `catch (Exception e) { return; }`  | Silencia falhas.                         | Sempre logue ou propague.                                    |
| `new Thread()`                     | Não gerenciável.                         | Use `VirtualThreadExecutor` ou `ExecutorService`.            |
| `synchronized` em métodos públicos | Lock contention.                         | Use `ConcurrentHashMap`, `AtomicX`, ou estruturas lock-free. |
| `instanceof` em cadeia longa       | Quebra OCP.                              | Use `switch` pattern matching (Java 25) ou polimorfismo.     |

---

*Este documento é um contrato. Se um code agent não conseguir segui-lo, deve abortar e pedir esclarecimento em vez de
assumir.*
