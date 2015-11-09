package ru.mipt.acsl.decode.model.domain.impl;

import org.jetbrains.annotations.NotNull;
import ru.mipt.acsl.decode.model.domain.*;
import ru.mipt.acsl.decode.model.domain.impl.type.AbstractDecodeOptionalInfoAware;
import scala.Option;

import java.util.List;
import java.util.Optional;

/**
 * @author Artem Shein
 */
public class ImmutableDecodeCommand extends AbstractDecodeOptionalInfoAware implements DecodeCommand
{
    @NotNull
    private final List<DecodeCommandArgument> arguments;
    @NotNull
    private final DecodeName name;
    @NotNull
    private final Option<Integer> id;
    @NotNull
    private final Option<DecodeMaybeProxy<DecodeType>> returnType;

    @NotNull
    public static DecodeCommand newInstance(@NotNull DecodeName name, @NotNull Option<Integer> id, @NotNull Option<String> info,
                                            @NotNull List<DecodeCommandArgument> arguments,
                                            @NotNull Option<DecodeMaybeProxy<DecodeType>> returnType)
    {
        return new ImmutableDecodeCommand(name, id, info, arguments, returnType);
    }

    @Override
    @NotNull
    public Option<DecodeMaybeProxy<DecodeType>> returnType()
    {
        return returnType;
    }

    @NotNull
    @Override
    public Option<Integer> id()
    {
        return id;
    }

    @NotNull
    @Override
    public List<DecodeCommandArgument> arguments()
    {
        return arguments;
    }

    @NotNull
    @Override
    public Option<DecodeName> optionalName()
    {
        return Option.apply(name);
    }

    @NotNull
    @Override
    public DecodeName name()
    {
        return name;
    }

    @Override
    public String toString()
    {
        return String.format("%s{name=%s, arguments=%s, info=%s}", ImmutableDecodeCommand.class.getName(), name,
                arguments, info());
    }

    private ImmutableDecodeCommand(@NotNull DecodeName name, @NotNull Option<Integer> id,
                                   @NotNull Option<String> info,
                                   @NotNull List<DecodeCommandArgument> arguments,
                                   @NotNull Option<DecodeMaybeProxy<DecodeType>> returnType)
    {
        super(info);
        this.name = name;
        this.id = id;
        this.arguments = arguments;
        this.returnType = returnType;
    }
}
