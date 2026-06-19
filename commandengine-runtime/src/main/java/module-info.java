module com.hanielfialho.runtime {
    exports com.hanielfialho.runtime;
    exports com.hanielfialho.runtime.telemetry;
    exports com.hanielfialho.runtime.util;

    requires transitive com.hanielfialho.api;
    requires com.github.benmanes.caffeine;
    requires java.base;
    requires java.logging;
    requires brigadier;
    requires static org.jetbrains.annotations;

    uses com.hanielfialho.api.command.CommandAdapterFactory;
}
