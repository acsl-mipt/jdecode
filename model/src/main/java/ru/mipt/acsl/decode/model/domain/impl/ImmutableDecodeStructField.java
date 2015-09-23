package ru.mipt.acsl.decode.model.domain.impl;

import ru.mipt.acsl.decode.model.domain.DecodeName;
import ru.mipt.acsl.decode.model.domain.proxy.DecodeMaybeProxy;
import ru.mipt.acsl.decode.model.domain.type.DecodeStructField;
import ru.mipt.acsl.decode.model.domain.type.DecodeType;
import ru.mipt.acsl.decode.model.domain.DecodeUnit;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * @author Artem Shein
 */
public class ImmutableDecodeStructField extends AbstractDecodeOptionalInfoAware implements DecodeStructField
{
    @NotNull
    private final DecodeMaybeProxy<DecodeType> type;
    @NotNull
    private final Optional<DecodeMaybeProxy<DecodeUnit>> unit;
    @NotNull
    private final DecodeName name;

    public static DecodeStructField newInstance(@NotNull DecodeName name, @NotNull DecodeMaybeProxy<DecodeType> type,
                                               @NotNull Optional<DecodeMaybeProxy<DecodeUnit>> unit,
                                               @NotNull Optional<String> info)
    {
        return new ImmutableDecodeStructField(name, type, unit, info);
    }

    private ImmutableDecodeStructField(@NotNull DecodeName name, @NotNull DecodeMaybeProxy<DecodeType> type,
                                       @NotNull Optional<DecodeMaybeProxy<DecodeUnit>> unit,
                                       @NotNull Optional<String> info)
    {
        super(info);
        this.name = name;
        this.type = type;
        this.unit = unit;
    }

    @NotNull
    @Override
    public DecodeMaybeProxy<DecodeType> getType()
    {
        return type;
    }

    @NotNull
    @Override
    public Optional<DecodeMaybeProxy<DecodeUnit>> getUnit()
    {
        return unit;
    }

    @NotNull
    @Override
    public DecodeName getName()
    {
        return name;
    }

    @NotNull
    @Override
    public Optional<DecodeName> getOptionalName()
    {
        return Optional.of(name);
    }

    @NotNull
    @Override
    public String toString()
    {
        return String.format("%s{name=%s, type=%s, unit=%s, info=%s}", ImmutableDecodeStructField.class.getName(), name,
                type, unit, info);
    }
}
