package ru.mipt.acsl.decode.model.domain.impl;

import ru.mipt.acsl.decode.model.domain.DecodeOptionalInfoAware;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * @author Artem Shein
 */
public abstract class AbstractDecodeOptionalInfoAware implements DecodeOptionalInfoAware
{
    @NotNull
    protected final Optional<String> info;

    public AbstractDecodeOptionalInfoAware(@NotNull Optional<String> info)
    {
        this.info = info;
    }

    @NotNull
    @Override
    public Optional<String> getInfo()
    {
        return info;
    }
}
