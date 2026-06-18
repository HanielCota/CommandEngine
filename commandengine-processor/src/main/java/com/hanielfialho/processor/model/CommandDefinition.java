package com.hanielfialho.processor.model;

import java.util.Objects;
import javax.lang.model.element.TypeElement;

public record CommandDefinition(TypeElement element, CommandModel model) {

    public CommandDefinition {
        Objects.requireNonNull(element, "element");
        Objects.requireNonNull(model, "model");
    }
}
