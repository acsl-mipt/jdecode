package ru.mipt.acsl.parsing;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * @author Artem Shein
 */
public class ParsingException extends RuntimeException
{
    public ParsingException(@NotNull String msg)
    {
        super(msg);
    }

    public ParsingException(@NotNull Throwable cause)
    {
        super(cause);
    }
}
