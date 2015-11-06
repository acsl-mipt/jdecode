package ru.mipt.acsl.decode.model.domain.impl;

import ru.mipt.acsl.decode.model.domain.DecodeComponent;
import ru.mipt.acsl.decode.model.domain.IDecodeName;
import ru.mipt.acsl.decode.model.domain.message.DecodeMessageParameter;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

/**
 * @author Artem Shein
 */
public abstract class AbstractImmutableDecodeMessage extends AbstractDecodeMessage
{
    @NotNull
    protected final DecodeName name;
    @NotNull
    protected final Optional<Integer> id;
    @NotNull
    protected final List<DecodeMessageParameter> parameters;
    @NotNull
    protected final DecodeComponent component;

    @NotNull
    @Override
    public DecodeComponent getComponent()
    {
        return component;
    }

    @Override
    @NotNull
    public Optional<Integer> getId()
    {
        return id;
    }

    @Override
    @NotNull
    public DecodeName getName()
    {
        return name;
    }

    @Override
    @NotNull
    public List<DecodeMessageParameter> getParameters()
    {
        return parameters;
    }

    @NotNull
    @Override
    public Optional<IDecodeName> getOptionalName()
    {
        return Optional.of(name);
    }


    protected AbstractImmutableDecodeMessage(@NotNull DecodeComponent component, @NotNull DecodeName name,
                                             @NotNull Optional<Integer> id,
                                             @NotNull Optional<String> info,
                                             @NotNull List<DecodeMessageParameter> parameters)
    {
        super(info);
        this.component = component;
        this.id = id;
        this.name = name;
        this.parameters = parameters;
    }
}
