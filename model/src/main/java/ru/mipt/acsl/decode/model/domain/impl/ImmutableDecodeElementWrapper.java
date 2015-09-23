package ru.mipt.acsl.decode.model.domain.impl;

import org.jetbrains.annotations.NotNull;
import ru.mipt.acsl.decode.model.domain.DecodeElement;

/**
 * @author Artem Shein
 */
public class ImmutableDecodeElementWrapper<T> implements DecodeElement
{
    private final T value;

    public static <T> ImmutableDecodeElementWrapper<T> newInstance(T value)
    {
        return new ImmutableDecodeElementWrapper<>(value);
    }

    public T getValue()
    {
        return value;
    }

    private ImmutableDecodeElementWrapper(@NotNull T value)
    {
        this.value = value;
    }
}
