package ru.mipt.acsl.decode.model.domain.impl;

import org.jetbrains.annotations.NotNull;
import ru.mipt.acsl.decode.model.domain.DecodeMessageParameter;

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
    public String value()
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
