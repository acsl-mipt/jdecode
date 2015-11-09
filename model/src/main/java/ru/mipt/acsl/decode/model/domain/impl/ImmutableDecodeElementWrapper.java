package ru.mipt.acsl.decode.model.domain.impl;

import org.jetbrains.annotations.NotNull;

/**
 * @author Artem Shein
 */
public class ImmutableDecodeElementWrapper<T>
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
