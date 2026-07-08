module com.hanielfialho.example.paper {
    requires com.hanielfialho.api;
    requires com.hanielfialho.runtime;
    requires com.hanielfialho.platform.paper;
    requires java.logging;
    requires org.bukkit;

    provides com.hanielfialho.api.command.CommandAdapterFactory with
            com.hanielfialho.example.paper.ExampleWarpCommandCommandAdapterFactory;
}
