package ru.mipt.acsl.decode.idea.plugin;

import ru.mipt.acsl.decode.modeling.TransformationMessage;
import org.jetbrains.annotations.NotNull;

/**
 * @author Artem Shein
 */
class DecodeTransformationMessage implements TransformationMessage
{
    @NotNull
    private final String text;
    @NotNull
    private final Level level;

    public DecodeTransformationMessage(@NotNull Level level, @NotNull String msg, @NotNull Object... params)
    {
        this.level = level;
        this.text = String.format(msg, params);
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
}
