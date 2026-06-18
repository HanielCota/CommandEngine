package com.hanielfialho.platform.paper.argument;

import java.util.function.Function;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

public final class MaterialArgumentResolver extends AbstractPaperArgumentResolver<Material> {

    public MaterialArgumentResolver() {
        this(Material::matchMaterial);
    }

    public MaterialArgumentResolver(@NotNull Function<String, Material> lookup) {
        super(Material.class, "material", lookup);
    }
}
