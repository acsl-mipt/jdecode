package ru.mipt.acsl.decode.model.domain.impl;

import org.jetbrains.annotations.NotNull;
import ru.mipt.acsl.decode.model.domain.DecodeName;
import ru.mipt.acsl.decode.model.domain.DecodeOptionalNameAware;

import java.util.Optional;

/**
 * @author Artem Shein
 */
public class AbstractDecodeNameAndOptionalInfoAware extends AbstractDecodeOptionalInfoAware implements
        DecodeOptionalNameAware
{
    @NotNull
    private final DecodeName name;

    protected AbstractDecodeNameAndOptionalInfoAware(@NotNull DecodeName name, @NotNull Optional<String> info)
    {
        super(info);
        this.name = name;
    }

    @NotNull
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
}
