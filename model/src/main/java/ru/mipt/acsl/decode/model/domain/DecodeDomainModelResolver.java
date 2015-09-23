package ru.mipt.acsl.decode.model.domain;

import ru.mipt.acsl.decode.model.domain.proxy.DecodeResolvingResult;
import org.jetbrains.annotations.NotNull;

/**
 * @author Artem Shein
 */
public interface DecodeDomainModelResolver
{
    DecodeResolvingResult<DecodeReferenceable> resolve(@NotNull DecodeRegistry decodeRegistry);
}
