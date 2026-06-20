package com.hanielfialho.processor.model;

import java.util.Objects;

public record SuggestionMethodModel(String methodName, boolean async) {

    public SuggestionMethodModel {
        Objects.requireNonNull(methodName, "methodName");
    }
}
