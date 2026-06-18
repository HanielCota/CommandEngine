package com.hanielfialho.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

final class DefaultArgumentResolverRegistryTest {

    @Test
    void resolvesNativeIntegerArguments() throws Exception {
        var registry = CommandEngine.defaultArgumentResolverRegistry();
        var resolver = registry.resolver(int.class).orElseThrow();
        var captured = new AtomicReference<Integer>();
        var dispatcher = new CommandDispatcher<Object>();

        dispatcher.register(LiteralArgumentBuilder.<Object>literal("root")
                .then(RequiredArgumentBuilder.argument("amount", resolver.argumentType())
                        .executes(context -> {
                            captured.set((Integer) resolver.resolve(context, "amount"));
                            return 1;
                        })));

        dispatcher.execute("root 42", new Object());

        assertThat(captured.get()).isEqualTo(42);
    }
}
