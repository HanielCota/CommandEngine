module com.hanielfialho.api {
    exports com.hanielfialho.api.annotation;
    exports com.hanielfialho.api.command;
    exports com.hanielfialho.api.source;
    exports com.hanielfialho.api.argument;
    exports com.hanielfialho.api.executor;
    exports com.hanielfialho.api.message;
    exports com.hanielfialho.api.rate;
    exports com.hanielfialho.api.result;
    exports com.hanielfialho.api.registry;
    exports com.hanielfialho.api.scheduler;
    exports com.hanielfialho.api.suggestion;
    exports com.hanielfialho.api.event;
    exports com.hanielfialho.api.telemetry;

    requires static org.jetbrains.annotations;
    requires transitive brigadier;
}
