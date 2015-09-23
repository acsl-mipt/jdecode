package ru.mipt.acsl.decode.model.domain.impl;

import org.jetbrains.annotations.NotNull;
import ru.mipt.acsl.decode.model.domain.type.ArraySize;

/**
 * @author Artem Shein
 */
public class ImmutableArraySize implements ArraySize
{
    private final long minLength;
    private final long maxLength;

    @NotNull
    public static ArraySize newInstance(long minLength, long maxLength)
    {
        return new ImmutableArraySize(minLength, maxLength);
    }

    @Override
    public long getMinLength()
    {
        return minLength;
    }

    @Override
    public long getMaxLength()
    {
        return maxLength;
    }

    @Override
    public String toString()
    {
        return String.format("ImmutableArraySize{minLength=%s, maxLength=%s}", minLength, maxLength);
    }

    public ImmutableArraySize(long minLength, long maxLength)
    {
        this.minLength = minLength;
        this.maxLength = maxLength;
    }
}
