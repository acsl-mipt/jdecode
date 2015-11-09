package ru.mipt.acsl.decode.model.domain.impl.type;

import ru.mipt.acsl.decode.model.domain.*;
import org.jetbrains.annotations.NotNull;
import scala.Option;

import java.util.Optional;

/**
 * @author Artem Shein
 */
public class ImmutableDecodeStructField extends AbstractDecodeOptionalInfoAware implements DecodeStructField
{
    @NotNull
    private final DecodeMaybeProxy<DecodeType> type;
    @NotNull
    private final Option<DecodeMaybeProxy<DecodeUnit>> unit;
    @NotNull
    private final DecodeName name;

    public static DecodeStructField newInstance(@NotNull DecodeName name, @NotNull DecodeMaybeProxy<DecodeType> type,
                                                @NotNull Option<DecodeMaybeProxy<DecodeUnit>> unit,
                                                @NotNull Option<String> info)
    {
        return new ImmutableDecodeStructField(name, type, unit, info);
    }

    private ImmutableDecodeStructField(@NotNull DecodeName name, @NotNull DecodeMaybeProxy<DecodeType> type,
                                       @NotNull Option<DecodeMaybeProxy<DecodeUnit>> unit,
                                       @NotNull Option<String> info)
    {
        super(info);
        this.name = name;
        this.type = type;
        this.unit = unit;
    }

    @NotNull
    @Override
    public DecodeMaybeProxy<DecodeType> fieldType()
    {
        return type;
    }

    @NotNull
    @Override
    public Option<DecodeMaybeProxy<DecodeUnit>> unit()
    {
        return unit;
    }

    @NotNull
    @Override
    public DecodeName name()
    {
        return name;
    }

    @NotNull
    @Override
    public Option<DecodeName> optionalName()
    {
        return Option.apply(name);
    }

    @NotNull
    @Override
    public String toString()
    {
        return String.format("%s{name=%s, type=%s, unit=%s, info=%s}", ImmutableDecodeStructField.class.getName(), name,
                type, unit, info());
    }
}
