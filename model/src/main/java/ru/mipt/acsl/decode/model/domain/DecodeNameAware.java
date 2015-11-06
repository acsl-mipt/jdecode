package ru.mipt.acsl.decode.model.domain;

import org.jetbrains.annotations.NotNull;

/**
 * @author Artem Shein
 */
public interface DecodeNameAware extends DecodeOptionalNameAware
{
    @NotNull
    default IDecodeName getName()
    {
        return getOptionalName().get();
    }
}
