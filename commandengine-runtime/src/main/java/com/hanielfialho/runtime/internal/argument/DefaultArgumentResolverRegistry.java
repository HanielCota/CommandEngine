/*
 * Copyright (c) 2026 Haniel Fialho
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
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
