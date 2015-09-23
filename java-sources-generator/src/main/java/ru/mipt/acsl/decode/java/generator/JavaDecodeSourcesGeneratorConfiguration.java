package ru.mipt.acsl.decode.java.generator;

import org.jetbrains.annotations.NotNull;
import org.kohsuke.args4j.Argument;
import ru.mipt.acsl.decode.model.domain.DecodeRegistry;

import java.io.File;

/**
 * @author Artem Shein
 */
public class JavaDecodeSourcesGeneratorConfiguration
{
    @NotNull
    private File outputDir;
    @NotNull
    private DecodeRegistry registry;

    public JavaDecodeSourcesGeneratorConfiguration(@NotNull File outputDir,
                                                   @NotNull DecodeRegistry registry)
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
    DecodeRegistry getRegistry()
    {
        return registry;
    }
}
