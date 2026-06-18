module com.hanielfialho.processor {
    requires java.compiler;
    requires com.hanielfialho.api;
    requires static org.jetbrains.annotations;

    provides javax.annotation.processing.Processor with
            com.hanielfialho.processor.CommandEngineProcessor;
}
