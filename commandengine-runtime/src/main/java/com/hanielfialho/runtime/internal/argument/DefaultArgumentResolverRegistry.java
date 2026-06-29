package com.hanielfialho.runtime.internal.argument;

import com.hanielfialho.api.argument.ArgumentResolverRegistry;
import com.hanielfialho.api.argument.ArgumentTypeResolver;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("java:S2201")
public final class DefaultArgumentResolverRegistry implements ArgumentResolverRegistry {

    private final Map<Class<?>, ArgumentTypeResolver<?>> resolvers = new ConcurrentHashMap<>();

    public DefaultArgumentResolverRegistry() {
        register(new StringArgumentResolver());
        registerPrimitive(new BooleanArgumentResolver(), boolean.class);
        registerPrimitive(new IntegerArgumentResolver(), int.class);
        registerPrimitive(new LongArgumentResolver(), long.class);
        registerPrimitive(new FloatArgumentResolver(), float.class);
        registerPrimitive(new DoubleArgumentResolver(), double.class);
    }

    @Override
    public @NotNull <T> ArgumentResolverRegistry register(@NotNull ArgumentTypeResolver<T> resolver) {
        Objects.requireNonNull(resolver, "resolver");
        resolvers.put(resolver.type(), resolver);
        return this;
    }

    @Override
    public @NotNull <T> Optional<ArgumentTypeResolver<T>> resolver(@NotNull Class<T> type) {
        Objects.requireNonNull(type, "type");
        @SuppressWarnings("unchecked")
        var resolver = (ArgumentTypeResolver<T>) resolvers.get(type);
        return Optional.ofNullable(resolver);
    }

    private <T> void registerPrimitive(ArgumentTypeResolver<T> resolver, Class<?> primitiveType) {
        register(resolver);
        resolvers.put(primitiveType, resolver);
    }
}
