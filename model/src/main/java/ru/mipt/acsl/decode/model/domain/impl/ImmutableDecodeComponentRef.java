package ru.mipt.acsl.decode.model.domain.impl;

import org.jetbrains.annotations.NotNull;
import ru.mipt.acsl.decode.model.domain.DecodeComponent;
import ru.mipt.acsl.decode.model.domain.DecodeComponentRef;
import ru.mipt.acsl.decode.model.domain.proxy.DecodeMaybeProxy;

import java.util.Optional;

/**
 * @author Artem Shein
 */
public class ImmutableDecodeComponentRef implements DecodeComponentRef
{
    @NotNull
    private final DecodeMaybeProxy<DecodeComponent> component;
    @NotNull
    private final Optional<String> alias;

    public static DecodeComponentRef newInstance(@NotNull DecodeMaybeProxy<DecodeComponent> componentProxy)
    {
        return new ImmutableDecodeComponentRef(Optional.empty(), componentProxy);
    }

    public static DecodeComponentRef newInstance(@NotNull String alias,
                                                 @NotNull DecodeMaybeProxy<DecodeComponent> maybeProxy)
    {
        return new ImmutableDecodeComponentRef(Optional.of(alias), maybeProxy);
    }

    @NotNull
    @Override
    public DecodeMaybeProxy<DecodeComponent> getComponent()
    {
        return component;
    }

    @NotNull
    @Override
    public Optional<String> getAlias()
    {
        return alias;
    }

    private ImmutableDecodeComponentRef(@NotNull Optional<String> alias,
                                        @NotNull DecodeMaybeProxy<DecodeComponent> componentProxy)
    {
        this.alias = alias;
        this.component = componentProxy;
    }
}
