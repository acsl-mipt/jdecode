package ru.mipt.acsl.decode.java.generator;

import org.jetbrains.annotations.NotNull;
import org.kohsuke.args4j.Argument;
import ru.mipt.acsl.decode.model.domain.impl.registry.Registry;

import java.io.File;

/**
 * @author Artem Shein
 */
public class JavaDecodeSourcesGeneratorConfiguration
{
    @NotNull
    private File outputDir;
    @NotNull
    private Registry registry;

    public JavaDecodeSourcesGeneratorConfiguration(@NotNull File outputDir,
                                                   @NotNull Registry registry)
    {
        this.outputDir = outputDir;
        this.registry = registry;
    }

    @NotNull
    @Argument(metaVar = "OUTPUT_DIR", required = true)
    File getOutputDir()
    {
        return outputDir;
    }

    @NotNull
    Registry getRegistry()
    {
        return registry;
    }
}
