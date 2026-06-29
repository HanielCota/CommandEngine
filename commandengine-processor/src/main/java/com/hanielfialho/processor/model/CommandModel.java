package com.hanielfialho.processor.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public final class CommandModel {

    private final String className;
    private final String name;
    private final String[] aliases;
    private final String description;
    private final String permission;
    private final List<SubcommandModel> subcommands = new ArrayList<>();

    public CommandModel(String className, String name, String[] aliases, String description, String permission) {
        this.className = Objects.requireNonNull(className, "className");
        this.name = Objects.requireNonNull(name, "name");
        this.aliases = Objects.requireNonNull(aliases, "aliases").clone();
        this.description = Objects.requireNonNull(description, "description");
        this.permission = Objects.requireNonNull(permission, "permission");
    }

    public void addSubcommand(SubcommandModel subcommand) {
        this.subcommands.add(Objects.requireNonNull(subcommand, "subcommand"));
    }

    public String getClassName() {
        return className;
    }

    public String getName() {
        return name;
    }

    public String[] getAliases() {
        return aliases.clone();
    }

    public String getDescription() {
        return description;
    }

    public String getPermission() {
        return permission;
    }

    public List<SubcommandModel> getSubcommands() {
        return List.copyOf(subcommands);
    }

    public String getSimpleClassName() {
        var lastDot = className.lastIndexOf('.');
        return lastDot >= 0 ? className.substring(lastDot + 1) : className;
    }

    public String getQualifiedClassName() {
        return className;
    }

    public String getAdapterClassName() {
        return getSimpleClassName().replace(".", "_") + "CommandAdapter";
    }

    public String getPackageName() {
        var lastDot = className.lastIndexOf('.');
        return lastDot >= 0 ? className.substring(0, lastDot) : "";
    }

    @Override
    public String toString() {
        return "CommandModel[className=" + className
                + ", name=" + name
                + ", aliases=" + Arrays.toString(aliases)
                + ", subcommands=" + subcommands.size()
                + "]";
    }
}
