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
