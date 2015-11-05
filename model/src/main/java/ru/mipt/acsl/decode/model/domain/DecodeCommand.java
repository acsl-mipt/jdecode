package ru.mipt.acsl.decode.model.domain;

import org.jetbrains.annotations.NotNull;
import ru.mipt.acsl.decode.model.domain.proxy.DecodeMaybeProxy;
import ru.mipt.acsl.decode.model.domain.type.DecodeType;

import java.util.List;
import java.util.Optional;

/**
 * @author Artem Shein
 */
public interface DecodeCommand extends DecodeOptionalInfoAware, DecodeNameAware
{
    @NotNull
    Optional<DecodeMaybeProxy<DecodeType>> getReturnType();
    @NotNull
    Optional<Integer> getId();
    @NotNull
    List<DecodeCommandArgument> getArguments();
}
