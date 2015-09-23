package ru.mipt.acsl.decode.model.domain.impl;

import ru.mipt.acsl.decode.model.domain.message.DecodeMessageParameter;
import org.jetbrains.annotations.NotNull;

/**
 * @author Artem Shein
 */
public class ImmutableDecodeAllParameters implements DecodeMessageParameter
{

    public static final ImmutableDecodeAllParameters INSTANCE = new ImmutableDecodeAllParameters();

    @NotNull
    @Override
    public String getValue()
    {
        return "*";
    }

    @Override
    public String toString()
    {
        return String.format("%s{value=*}", ImmutableDecodeAllParameters.class.getName());
    }

    private ImmutableDecodeAllParameters()
    {

    }
}
