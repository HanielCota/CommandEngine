package com.hanielfialho.api.source;

import org.jetbrains.annotations.NotNull;

public interface PermissionHolder {

    boolean hasPermission(@NotNull String permission);
}
