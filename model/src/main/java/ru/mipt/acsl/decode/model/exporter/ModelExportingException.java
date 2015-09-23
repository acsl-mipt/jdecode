package ru.mipt.acsl.decode.model.exporter;

import org.jetbrains.annotations.NotNull;

/**
 * @author Artem Shein
 */
public class ModelExportingException extends RuntimeException
{
    public ModelExportingException(@NotNull Throwable e)
    {
        super(e);
    }

    public ModelExportingException(@NotNull String msg)
    {
        super(msg);
    }

    public ModelExportingException(@NotNull Throwable cause, @NotNull String msg, @NotNull Object... args)
    {
        super(String.format(msg, args), cause);
    }
}
