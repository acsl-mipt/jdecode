package ru.mipt.acsl.decode.model.domain.impl.type;

import org.jetbrains.annotations.NotNull;
import ru.mipt.acsl.decode.model.domain.DecodeEnumConstant;
import ru.mipt.acsl.decode.model.domain.DecodeName;
import scala.Option;

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
    public static DecodeEnumConstant newInstance(@NotNull DecodeName name, @NotNull String value, @NotNull Option<String> info)
    {
        return new ImmutableDecodeEnumConstant(name, value, info);
    }

    @NotNull
    public static DecodeEnumConstant newInstanceWrapper(@NotNull DecodeName name,
                                                       @NotNull String value,
                                                       @NotNull Option<String> info)
    {
        return newInstance(name, value, info);
    }

    @NotNull
    @Override
    public DecodeName name()
    {
        return name;
    }

    @NotNull
    @Override
    public String value()
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
        return this == c || (name.equals(c.name()) && value.equals(c.value()));
    }

    @Override
    public String toString()
    {
        return String.format("%s{name=%s, value=%s, info=%s}", ImmutableDecodeEnumConstant.class.getName(), name, value,
                info());
    }

    private ImmutableDecodeEnumConstant(@NotNull DecodeName name, @NotNull String value, @NotNull Option<String> info)
    {
        super(info);
        this.name = name;
        this.value = value;
    }
}
