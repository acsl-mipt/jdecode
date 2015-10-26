package ru.mipt.acsl.decode.model.domain.impl;

import ru.mipt.acsl.decode.model.domain.DecodeName;
import ru.mipt.acsl.decode.model.domain.DecodeCommand;
import ru.mipt.acsl.decode.model.domain.DecodeCommandArgument;
import org.jetbrains.annotations.NotNull;
import ru.mipt.acsl.decode.model.domain.DecodeReferenceable;
import ru.mipt.acsl.decode.model.domain.proxy.DecodeMaybeProxy;

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
    private final DecodeMaybeProxy<DecodeReferenceable> returnType;

    @NotNull
    public static DecodeCommand newInstance(@NotNull DecodeName name, @NotNull Optional<Integer> id, @NotNull Optional<String> info,
                                            @NotNull List<DecodeCommandArgument> arguments,
                                            @NotNull DecodeMaybeProxy<DecodeReferenceable> returnType)
    {
        return new ImmutableDecodeCommand(name, id, info, arguments, returnType);
    }

    private ImmutableDecodeCommand(@NotNull DecodeName name, @NotNull Optional<Integer> id,
                                   @NotNull Optional<String> info,
                                   @NotNull List<DecodeCommandArgument> arguments,
                                   @NotNull DecodeMaybeProxy<DecodeReferenceable> returnType)
    {
        super(info);
        this.name = name;
        this.id = id;
        this.arguments = arguments;
        this.returnType = returnType;
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
    public Optional<DecodeName> getOptionalName()
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
}
