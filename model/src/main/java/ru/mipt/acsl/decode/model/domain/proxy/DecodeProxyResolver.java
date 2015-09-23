package ru.mipt.acsl.decode.model.domain.proxy;

import ru.mipt.acsl.decode.model.domain.DecodeReferenceable;
import ru.mipt.acsl.decode.model.domain.DecodeRegistry;
import org.jetbrains.annotations.NotNull;

import java.net.URI;

/**
 * @author Artem Shein
 */
public interface DecodeProxyResolver
{
    @NotNull
    <T extends DecodeReferenceable> DecodeResolvingResult<T> resolve(@NotNull DecodeRegistry registry, @NotNull URI uri,
                                                       @NotNull Class<T> cls);
}
