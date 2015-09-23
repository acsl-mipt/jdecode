package ru.mipt.acsl.decode.mavlink.generator;

import org.jetbrains.annotations.NotNull;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

import java.io.File;

/**
 * @author Artem Shein
 */
public class MavlinkDecodeSourceGeneratorConfiguration
{
    @NotNull
    @Argument(required = true, index = 1, metaVar = "OUTPUT_FILE")
    private File outputFile;

    @NotNull
    @Argument(required = true, metaVar = "INPUT_FILE")
    private File inputFile;

    @NotNull
    @Option(name = "--namespace", aliases = {"-n"}, usage = "Decode namespace")
    private String namespace = "mavlink";

    @NotNull
    public String getNamespace()
    {
        return namespace;
    }

    @NotNull
    public File getInputFile()
    {
        return inputFile;
    }

    @NotNull
    public File getOutputFile()
    {
        return outputFile;
    }
}
