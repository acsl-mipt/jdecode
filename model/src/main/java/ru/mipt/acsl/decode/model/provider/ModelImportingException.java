package ru.mipt.acsl.decode.model.provider;

import org.jetbrains.annotations.NotNull;

/**
 * @author Artem Shein
 */
public class ModelImportingException extends RuntimeException
{
    public ModelImportingException(@NotNull Throwable e)
    {
        super(e);
    }

    public ModelImportingException(@NotNull String msg)
    {
        super(msg);
    }

    public ModelImportingException(@NotNull Throwable cause, @NotNull String msg, @NotNull Object... args)
    {
        super(String.format(msg, args), cause);
    }
}
