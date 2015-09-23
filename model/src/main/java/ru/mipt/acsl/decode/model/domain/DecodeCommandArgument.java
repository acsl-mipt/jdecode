package ru.mipt.acsl.decode.model.domain;

import ru.mipt.acsl.decode.model.domain.proxy.DecodeMaybeProxy;
import ru.mipt.acsl.decode.model.domain.type.DecodeType;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * @author Artem Shein
 */
public interface DecodeCommandArgument extends DecodeNameAware, DecodeOptionalInfoAware
{
    @NotNull
    Optional<DecodeMaybeProxy<DecodeUnit>> getUnit();

    @NotNull
    DecodeMaybeProxy<DecodeType> getType();
}
