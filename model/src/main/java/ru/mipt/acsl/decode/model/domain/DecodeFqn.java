package ru.mipt.acsl.decode.model.domain;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author Artem Shein
 */
public interface DecodeFqn extends DecodeElement
{
    @NotNull
    List<IDecodeName> getParts();

    @NotNull
    String asString();

    @NotNull
    default IDecodeName getLast()
    {
        List<IDecodeName> parts = getParts();
        return parts.get(parts.size() - 1);
    }

    @NotNull
    DecodeFqn copyDropLast();

    int size();

    default boolean isEmpty()
    {
        return size() == 0;
    }
}
