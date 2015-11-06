package ru.mipt.acsl.decode.model.domain.impl;

import org.jetbrains.annotations.NotNull;
import ru.mipt.acsl.decode.model.domain.DecodeOptionalNameAware;
import ru.mipt.acsl.decode.model.domain.IDecodeName;

import java.util.Optional;

/**
 * @author Artem Shein
 */
public class AbstractDecodeNameAndOptionalInfoAware extends AbstractDecodeOptionalInfoAware implements
        DecodeOptionalNameAware
{
    @NotNull
    private final IDecodeName name;

    protected AbstractDecodeNameAndOptionalInfoAware(@NotNull IDecodeName name, @NotNull Optional<String> info)
    {
        super(info);
        this.name = name;
    }

    @NotNull
    public IDecodeName getName()
    {
        return name;
    }

    @NotNull
    @Override
    public Optional<IDecodeName> getOptionalName()
    {
        return Optional.of(name);
    }
}
