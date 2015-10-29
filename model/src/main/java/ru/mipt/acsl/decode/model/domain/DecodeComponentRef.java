package ru.mipt.acsl.decode.model.domain;

import org.jetbrains.annotations.NotNull;
import ru.mipt.acsl.decode.model.domain.proxy.DecodeMaybeProxy;

import java.util.Optional;

/**
 * @author Artem Shein
 */
public interface DecodeComponentRef
{
    @NotNull
    DecodeMaybeProxy<DecodeComponent> getComponent();

    @NotNull
    Optional<String> getAlias();
}
