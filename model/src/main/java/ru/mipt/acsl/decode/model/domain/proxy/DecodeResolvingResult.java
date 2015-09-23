package ru.mipt.acsl.decode.model.domain.proxy;

import ru.mipt.acsl.decode.model.domain.DecodeReferenceable;
import ru.mipt.acsl.decode.modeling.ResolvingResult;

import java.util.Optional;

/**
 * @author Artem Shein
 */
public interface DecodeResolvingResult<T extends DecodeReferenceable> extends ResolvingResult
{
    Optional<T> getResolvedObject();
}
