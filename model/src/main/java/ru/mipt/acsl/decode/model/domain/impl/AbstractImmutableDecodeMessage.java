package ru.mipt.acsl.decode.model.domain.impl;

import ru.mipt.acsl.decode.model.domain.AbstractDecodeMessage;
import ru.mipt.acsl.decode.model.domain.DecodeComponent;
import ru.mipt.acsl.decode.model.domain.DecodeName;
import ru.mipt.acsl.decode.model.domain.message.DecodeMessageParameter;
import org.jetbrains.annotations.NotNull;
import scala.Option;
import scala.collection.Seq;

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
    protected final Option<Integer> id;
    @NotNull
    protected final Seq<DecodeMessageParameter> parameters;
    @NotNull
    protected final DecodeComponent component;

    @NotNull
    @Override
    public DecodeComponent component()
    {
        return component;
    }

    @Override
    @NotNull
    public Option<Integer> id()
    {
        return id;
    }

    @Override
    @NotNull
    public DecodeName name()
    {
        return name;
    }

    @Override
    @NotNull
    public Seq<DecodeMessageParameter> parameters()
    {
        return parameters;
    }

    @NotNull
    @Override
    public Option<DecodeName> optionalName()
    {
        return Option.apply(name);
    }


    protected AbstractImmutableDecodeMessage(@NotNull DecodeComponent component, @NotNull DecodeName name,
                                             @NotNull Option<Integer> id,
                                             @NotNull Option<String> info,
                                             @NotNull Seq<DecodeMessageParameter> parameters)
    {
        super(info);
        this.component = component;
        this.id = id;
        this.name = name;
        this.parameters = parameters;
    }
}
