package ru.mipt.acsl.decode.model.domain;

import ru.mipt.acsl.decode.model.domain.proxy.DecodeResolvingResult;
import org.jetbrains.annotations.NotNull;

import java.net.URI;

/**
 * @author Artem Shein
 */
public interface DecodeProxy<T extends DecodeReferenceable>
{
    DecodeResolvingResult<T> resolve(@NotNull DecodeRegistry registry, @NotNull Class<T> cls);

    @NotNull
    URI getUri();
}
