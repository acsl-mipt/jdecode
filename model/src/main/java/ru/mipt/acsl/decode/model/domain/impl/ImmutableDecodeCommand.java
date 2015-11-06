package ru.mipt.acsl.decode.model.domain.impl;

import org.jetbrains.annotations.NotNull;
import ru.mipt.acsl.decode.model.domain.DecodeCommand;
import ru.mipt.acsl.decode.model.domain.DecodeCommandArgument;
import ru.mipt.acsl.decode.model.domain.IDecodeName;
import ru.mipt.acsl.decode.model.domain.proxy.DecodeMaybeProxy;
import ru.mipt.acsl.decode.model.domain.type.DecodeType;

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
    private final Optional<Integer> id;
    @NotNull
    private final Optional<DecodeMaybeProxy<DecodeType>> returnType;

    @NotNull
    public static DecodeCommand newInstance(@NotNull DecodeName name, @NotNull Optional<Integer> id, @NotNull Optional<String> info,
                                            @NotNull List<DecodeCommandArgument> arguments,
                                            @NotNull Optional<DecodeMaybeProxy<DecodeType>> returnType)
    {
        return new ImmutableDecodeCommand(name, id, info, arguments, returnType);
    }

    @Override
    @NotNull
    public Optional<DecodeMaybeProxy<DecodeType>> getReturnType()
    {
        return returnType;
    }

    @NotNull
    @Override
    public Optional<Integer> getId()
    {
        return id;
    }

    @NotNull
    @Override
    public List<DecodeCommandArgument> getArguments()
    {
        return arguments;
    }

    @NotNull
    @Override
    public Optional<IDecodeName> getOptionalName()
    {
        return Optional.of(name);
    }

    @NotNull
    @Override
    public DecodeName getName()
    {
        return name;
    }

    @Override
    public String toString()
    {
        return String.format("%s{name=%s, arguments=%s, info=%s}", ImmutableDecodeCommand.class.getName(), name,
                arguments, info);
    }

    private ImmutableDecodeCommand(@NotNull DecodeName name, @NotNull Optional<Integer> id,
                                   @NotNull Optional<String> info,
                                   @NotNull List<DecodeCommandArgument> arguments,
                                   @NotNull Optional<DecodeMaybeProxy<DecodeType>> returnType)
    {
        super(info);
        this.name = name;
        this.id = id;
        this.arguments = arguments;
        this.returnType = returnType;
    }
}
