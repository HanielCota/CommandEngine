module com.hanielfialho.test {
    exports com.hanielfialho.test;
    exports com.hanielfialho.test.brigadier;
    exports com.hanielfialho.test.command;
    exports com.hanielfialho.test.engine;
    exports com.hanielfialho.test.source;

    requires com.hanielfialho.api;
    requires com.hanielfialho.runtime;
    requires brigadier;
    requires static org.jetbrains.annotations;
}
