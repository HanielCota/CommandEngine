module com.hanielfialho.test {
    exports com.hanielfialho.test.brigadier;
    exports com.hanielfialho.test.command;
    exports com.hanielfialho.test.engine;
    exports com.hanielfialho.test.examples;
    exports com.hanielfialho.test.source;

    requires com.hanielfialho.api;
    requires com.hanielfialho.runtime;
    requires brigadier;
    requires java.logging;
    requires static org.jetbrains.annotations;

    provides com.hanielfialho.api.command.CommandAdapterFactory with
            com.hanielfialho.test.command.IntegrationDebugCommandCommandAdapterFactory,
            com.hanielfialho.test.command.IntegrationEconomyCommandCommandAdapterFactory,
            com.hanielfialho.test.command.IntegrationOptionalCustomCommandCommandAdapterFactory,
            com.hanielfialho.test.command.IntegrationPlayerOnlyCommandCommandAdapterFactory,
            com.hanielfialho.test.command.IntegrationSharedPrefixCommandCommandAdapterFactory,
            com.hanielfialho.test.command.IntegrationSlowSuggestionCommandCommandAdapterFactory,
            com.hanielfialho.test.command.IntegrationStressCommandCommandAdapterFactory,
            com.hanielfialho.test.command.IntegrationTimeCommandCommandAdapterFactory,
            com.hanielfialho.test.command.IntegrationWarpCommandCommandAdapterFactory,
            com.hanielfialho.test.examples.WarpCommandExampleCommandAdapterFactory;
}
