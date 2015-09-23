package ru.mipt.acsl.decode.modeling;

import org.jetbrains.annotations.NotNull;

/**
 * @author Artem Shein
 */
public interface ModelingMessage
{
    enum Level
    {
        NOTICE, WARN, ERROR
    }

    @NotNull
    String getText();

    @NotNull
    Level getLevel();
}
