package ru.mipt.acsl.decode.model.domain.impl;

import ru.mipt.acsl.decode.model.domain.IDecodeName;
import ru.mipt.acsl.decode.model.domain.DecodeOptionalNameAndOptionalInfoAware;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * @author Artem Shein
 */
public class AbstractDecodeOptionalNameAndOptionalInfoAware extends AbstractDecodeOptionalInfoAware implements
        DecodeOptionalNameAndOptionalInfoAware
{
    @NotNull
    protected Optional<IDecodeName> name;

    public AbstractDecodeOptionalNameAndOptionalInfoAware(@NotNull Optional<IDecodeName> name,
                                                          @NotNull Optional<String> info)
    {
        super(info);
        this.name = name;
    }

    @NotNull
    @Override
    public Optional<IDecodeName> getOptionalName()
    {
        return name;
    }
}
