package ru.mipt.acsl.decode.modeling.impl;

import ru.mipt.acsl.decode.modeling.ModelingMessage;
import ru.mipt.acsl.decode.modeling.ResolvingMessage;
import ru.mipt.acsl.decode.modeling.TransformationMessage;
import org.jetbrains.annotations.NotNull;

/**
 * @author Artem Shein
 */
public class SimpleMessage implements ModelingMessage, ResolvingMessage, TransformationMessage
{
    @NotNull
    private final Level level;
    @NotNull
    private final String text;

    public static SimpleMessage error(@NotNull String msg, Object... params)
    {
        return message(Level.ERROR, String.format(msg, params));
    }

    public static SimpleMessage warn(@NotNull String msg, Object... params)
    {
        return message(Level.WARN, String.format(msg, params));
    }

    public static SimpleMessage notice(@NotNull String msg, Object... params)
    {
        return message(Level.NOTICE, String.format(msg, params));
    }

    public static SimpleMessage message(@NotNull Level level, @NotNull String msg, @NotNull Object... params)
    {
        return message(level, String.format(msg, params));
    }

    public static SimpleMessage message(@NotNull Level level, @NotNull String text)
    {
        return new SimpleMessage(level, text);
    }

    @NotNull
    @Override
    public String getText()
    {
        return text;
    }

    @NotNull
    @Override
    public Level getLevel()
    {
        return level;
    }

    private SimpleMessage(@NotNull Level level, @NotNull String text)
    {
        this.level = level;
        this.text = text;
    }
}
