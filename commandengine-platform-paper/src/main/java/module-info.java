module com.hanielfialho.platform.paper {
    exports com.hanielfialho.platform.paper;
    exports com.hanielfialho.platform.paper.argument;
    exports com.hanielfialho.platform.paper.config;
    exports com.hanielfialho.platform.paper.scheduler;
    exports com.hanielfialho.platform.paper.suggestion;

    requires transitive com.hanielfialho.api;
    requires transitive com.hanielfialho.runtime;
    requires com.github.benmanes.caffeine;
    requires org.bukkit;
    requires brigadier;
    requires java.logging;
    requires static org.jetbrains.annotations;
    requires transitive net.kyori.adventure.api;
    requires transitive com.google.common;
}
