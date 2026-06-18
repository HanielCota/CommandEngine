package com.hanielfialho.processor.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class SubcommandModel {

    private final String methodName;
    private final String path;
    private final String permission;
    private final String description;
    private final boolean async;
    private final boolean returnsVoid;
    private final List<ParameterModel> parameters = new ArrayList<>();

    public SubcommandModel(
            String methodName, String path, String permission, String description, boolean async, boolean returnsVoid) {
        this.methodName = Objects.requireNonNull(methodName, "methodName");
        this.path = Objects.requireNonNull(path, "path");
        this.permission = Objects.requireNonNull(permission, "permission");
        this.description = Objects.requireNonNull(description, "description");
        this.async = async;
        this.returnsVoid = returnsVoid;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getPath() {
        return path;
    }

    public String getPermission() {
        return permission;
    }

    public String getDescription() {
        return description;
    }

    public boolean isAsync() {
        return async;
    }

    public boolean returnsVoid() {
        return returnsVoid;
    }

    public void addParameter(ParameterModel parameter) {
        this.parameters.add(Objects.requireNonNull(parameter, "parameter"));
    }

    public List<ParameterModel> getParameters() {
        return List.copyOf(parameters);
    }
}
