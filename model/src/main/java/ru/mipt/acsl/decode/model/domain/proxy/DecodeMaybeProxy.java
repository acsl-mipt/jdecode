package ru.mipt.acsl.decode.model.domain.proxy;

import org.jetbrains.annotations.NotNull;
import ru.mipt.acsl.decode.model.domain.DecodeProxy;
import ru.mipt.acsl.decode.model.domain.DecodeReferenceable;
import ru.mipt.acsl.decode.model.domain.DecodeRegistry;

/**
 * @author Artem Shein
 */
public interface DecodeMaybeProxy<T extends DecodeReferenceable>
{
    DecodeResolvingResult<T> resolve(@NotNull DecodeRegistry registry, @NotNull Class<T> cls);

    boolean isProxy();

    default boolean isResolved()
    {
        return !isProxy();
    }

    @NotNull
    T getObject();

    @NotNull
    DecodeProxy<T> getProxy();
}
