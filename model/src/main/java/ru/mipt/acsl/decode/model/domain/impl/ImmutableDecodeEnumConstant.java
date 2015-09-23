package ru.mipt.acsl.decode.model.domain.impl;

import ru.mipt.acsl.decode.model.domain.DecodeName;
import ru.mipt.acsl.decode.model.domain.type.DecodeEnumConstant;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * @author Artem Shein
 */
public class ImmutableDecodeEnumConstant extends AbstractDecodeOptionalInfoAware implements DecodeEnumConstant
{
    @NotNull
    private final DecodeName name;
    @NotNull
    private final String value;

    @NotNull
    public static DecodeEnumConstant newInstance(@NotNull DecodeName name, @NotNull String value, @NotNull Optional<String> info)
    {
        return new ImmutableDecodeEnumConstant(name, value, info);
    }

    @NotNull
    public static DecodeEnumConstant newInstanceWrapper(@NotNull DecodeName name,
                                                       @NotNull ImmutableDecodeElementWrapper<String> value,
                                                       @NotNull Optional<String> info)
    {
        return newInstance(name, value.getValue(), info);
    }

    @NotNull
    @Override
    public DecodeName getName()
    {
        return name;
    }

    @NotNull
    @Override
    public String getValue()
    {
        return value;
    }

    @Override
    public boolean equals(Object o)
    {
        if (o == null || !(o instanceof DecodeEnumConstant))
        {
            return false;
        }
        DecodeEnumConstant c = (DecodeEnumConstant)o;
        return this == c || (name.equals(c.getName()) && value.equals(c.getValue()));
    }

    @Override
    public String toString()
    {
        return String.format("%s{name=%s, value=%s, info=%s}", ImmutableDecodeEnumConstant.class.getName(), name, value,
                info);
    }

    private ImmutableDecodeEnumConstant(@NotNull DecodeName name, @NotNull String value, @NotNull Optional<String> info)
    {
        super(info);
        this.name = name;
        this.value = value;
    }
}
