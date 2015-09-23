package ru.mipt.acsl.decode.model.domain.impl;

import org.jetbrains.annotations.NotNull;
import ru.mipt.acsl.decode.model.domain.message.DecodeMessage;

import java.util.Optional;

/**
 * @author Artem Shein
 */
public abstract class AbstractDecodeMessage extends AbstractDecodeOptionalInfoAware implements
        DecodeMessage
{
    public AbstractDecodeMessage(@NotNull Optional<String> info)
    {
        super(info);
    }
}
