package com.hanielfialho.test.source;

import com.hanielfialho.api.source.CommandSource;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.jetbrains.annotations.NotNull;

public class MockCommandSource implements CommandSource {

    private final String name;
    private final Set<String> permissions = new HashSet<>();
    private final List<String> messages = new ArrayList<>();

    public MockCommandSource(@NotNull String name) {
        this.name = Objects.requireNonNull(name, "name");
    }

    public void addPermission(@NotNull String permission) {
        permissions.add(Objects.requireNonNull(permission, "permission"));
    }

    public @NotNull List<String> messages() {
        return List.copyOf(messages);
    }

    @Override
    public boolean hasPermission(@NotNull String permission) {
        Objects.requireNonNull(permission, "permission");
        return permission.isEmpty() || permissions.contains(permission);
    }

    @Override
    public @NotNull Object getHandle() {
        return this;
    }

    @Override
    public void sendMessage(@NotNull String message) {
        messages.add(Objects.requireNonNull(message, "message"));
    }

    @Override
    public @NotNull String getName() {
        return name;
    }
}
