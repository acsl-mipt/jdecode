package ru.mipt.acsl.decode.model.domain.impl;

import ru.mipt.acsl.decode.model.domain.message.DecodeMessageParameter;
import org.jetbrains.annotations.NotNull;

/**
 * @author Artem Shein
 */
public class ImmutableDecodeDeepAllParameters implements DecodeMessageParameter
{
    public static final ImmutableDecodeDeepAllParameters INSTANCE = new ImmutableDecodeDeepAllParameters();

    @NotNull
    @Override
    public String getValue()
    {
        return "*.*";
    }

    @Override
    public String toString()
    {
        return String.format("%s{value=*}", ImmutableDecodeDeepAllParameters.class.getName());
    }

    private ImmutableDecodeDeepAllParameters()
    {

    }
}
