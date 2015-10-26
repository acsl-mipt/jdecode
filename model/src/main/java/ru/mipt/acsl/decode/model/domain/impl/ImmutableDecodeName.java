package ru.mipt.acsl.decode.model.domain.impl;

import com.google.common.base.Preconditions;
import ru.mipt.acsl.decode.model.domain.DecodeName;
import org.jetbrains.annotations.NotNull;

/**
 * @author Artem Shein
 */
public final class ImmutableDecodeName implements DecodeName
{
    @NotNull
    private final String value;

    public static DecodeName newInstanceFromSourceName(@NotNull String name)
    {
        return new ImmutableDecodeName(DecodeName.mangleName(Preconditions.checkNotNull(name)));
    }

    public static DecodeName newInstanceFromMangledName(@NotNull String value)
    {
        return new ImmutableDecodeName(Preconditions.checkNotNull(value));
    }

    @NotNull
    @Override
    public String asString()
    {
        return value;
    }

    @Override
    public boolean equals(Object o)
    {
        if (o == null || !(o instanceof DecodeName))
        {
            return false;
        }
        DecodeName name = (DecodeName)o;
        return value.equals(name.asString());
    }

    @Override
    public int hashCode()
    {
        return value.hashCode();
    }

    @NotNull
    @Override
    public String toString()
    {
        return String.format("%s{value=%s}", ImmutableDecodeName.class.getName(), value);
    }

    private ImmutableDecodeName(@NotNull String value)
    {
        Preconditions.checkArgument(!value.contains(" ") && !value.contains("^"),
                "invalid mangled name '%s'", value);
        this.value = value;
    }
}
