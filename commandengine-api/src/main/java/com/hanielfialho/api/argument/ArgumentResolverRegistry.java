package com.hanielfialho.api.argument;

import java.util.Optional;
import org.jetbrains.annotations.NotNull;

public interface ArgumentResolverRegistry {

    @NotNull
    <T> ArgumentResolverRegistry register(@NotNull ArgumentTypeResolver<T> resolver);

    @NotNull
    <T> Optional<ArgumentTypeResolver<T>> resolver(@NotNull Class<T> type);
}
