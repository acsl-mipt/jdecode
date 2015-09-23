package ru.mipt.acsl.decode.model.domain.type;

import ru.mipt.acsl.decode.model.domain.DecodeOptionalInfoAware;
import ru.mipt.acsl.decode.model.domain.proxy.DecodeMaybeProxy;
import ru.mipt.acsl.decode.model.domain.DecodeNameAware;
import ru.mipt.acsl.decode.model.domain.DecodeUnit;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * @author Artem Shein
 */
public interface DecodeStructField extends DecodeNameAware, DecodeOptionalInfoAware
{
    @NotNull
    DecodeMaybeProxy<DecodeType> getType();

    @NotNull
    Optional<DecodeMaybeProxy<DecodeUnit>> getUnit();
}
