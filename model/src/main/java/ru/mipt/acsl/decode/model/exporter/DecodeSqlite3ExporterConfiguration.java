package ru.mipt.acsl.decode.model.exporter;

import org.jetbrains.annotations.NotNull;
import org.kohsuke.args4j.Argument;

import java.io.File;

/**
 * @author Artem Shein
 */
public class DecodeSqlite3ExporterConfiguration
{
    @NotNull
    @Argument(required = true, metaVar = "OUTPUT_FILE", usage = "Output file")
    private File outputFile;

    @NotNull
    public File getOutputFile()
    {
        return outputFile;
    }

    public void setOutputFile(@NotNull File outputFile)
    {
        this.outputFile = outputFile;
    }
}
