package ru.mipt.acsl.decode.model.domain.impl;

import ru.mipt.acsl.decode.model.domain.message.DecodeMessageParameter;
import org.jetbrains.annotations.NotNull;

/**
 * @author Artem Shein
 */
public class ImmutableDecodeMessageParameter implements DecodeMessageParameter
{
    @NotNull
    private final String text;

    public static DecodeMessageParameter newInstance(@NotNull String text)
    {
        return new ImmutableDecodeMessageParameter(text);
    }

    private ImmutableDecodeMessageParameter(@NotNull String text)
    {
        this.text = text;
    }

    @Override
    @NotNull
    public String getValue()
    {
        return text;
    }

    @NotNull
    @Override
    public String toString()
    {
        return String.format("%s{text=%s}", ImmutableDecodeMessageParameter.class.getName(), text);
    }
}
