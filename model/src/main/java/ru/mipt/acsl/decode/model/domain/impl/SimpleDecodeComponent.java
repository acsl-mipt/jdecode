package ru.mipt.acsl.decode.model.domain.impl;

import org.jetbrains.annotations.NotNull;
import ru.mipt.acsl.decode.model.domain.DecodeCommand;
import ru.mipt.acsl.decode.model.domain.DecodeComponent;
import ru.mipt.acsl.decode.model.domain.DecodeName;
import ru.mipt.acsl.decode.model.domain.DecodeNamespace;
import ru.mipt.acsl.decode.model.domain.message.DecodeMessage;
import ru.mipt.acsl.decode.model.domain.proxy.DecodeMaybeProxy;
import ru.mipt.acsl.decode.model.domain.type.DecodeType;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * @author Artem Shein
 */
public class SimpleDecodeComponent extends AbstractDecodeOptionalInfoAware implements DecodeComponent
{
    @NotNull
    private final DecodeName name;
    @NotNull
    private final Optional<Integer> id;
    @NotNull
    private DecodeNamespace namespace;
    @NotNull
    private final Optional<DecodeMaybeProxy<DecodeType>> baseType;
    @NotNull
    private final Set<DecodeMaybeProxy<DecodeComponent>> subComponents;
    @NotNull
    private final List<DecodeCommand> commands;
    @NotNull
    private final List<DecodeMessage> messages;

    @NotNull
    @Override
    public Optional<DecodeMaybeProxy<DecodeType>> getBaseType()
    {
        return baseType;
    }

    @NotNull
    @Override
    public Set<DecodeMaybeProxy<DecodeComponent>> getSubComponents()
    {
        return subComponents;
    }

    @NotNull
    @Override
    public List<DecodeCommand> getCommands()
    {
        return commands;
    }

    @NotNull
    @Override
    public List<DecodeMessage> getMessages()
    {
        return messages;
    }

    @NotNull
    @Override
    public DecodeNamespace getNamespace()
    {
        return namespace;
    }

    @Override
    public void setNamespace(@NotNull DecodeNamespace namespace)
    {
        this.namespace = namespace;
    }

    @NotNull
    @Override
    public Optional<String> getInfo()
    {
        return info;
    }

    @NotNull
    @Override
    public Optional<DecodeName> getOptionalName()
    {
        return Optional.of(name);
    }

    public static DecodeComponent newInstance(@NotNull DecodeName name, @NotNull DecodeNamespace namespace,
                                             @NotNull Optional<Integer> id,
                                             @NotNull Optional<DecodeMaybeProxy<DecodeType>> baseType,
                                             @NotNull Optional<String> info,
                                             @NotNull Set<DecodeMaybeProxy<DecodeComponent>> subComponents,
                                             @NotNull List<DecodeCommand> commands,
                                             @NotNull List<DecodeMessage> messages)
    {
        return new SimpleDecodeComponent(name, namespace, id, baseType, info, subComponents, commands, messages);
    }

    private SimpleDecodeComponent(@NotNull DecodeName name, @NotNull DecodeNamespace namespace,
                                  @NotNull Optional<Integer> id, @NotNull Optional<DecodeMaybeProxy<DecodeType>> baseType,
                                  @NotNull Optional<String> info,
                                  @NotNull Set<DecodeMaybeProxy<DecodeComponent>> subComponents,
                                  @NotNull List<DecodeCommand> commands, @NotNull List<DecodeMessage> messages)
    {
        super(info);
        this.name = name;
        this.namespace = namespace;
        this.id = id;
        this.baseType = baseType;
        this.subComponents = subComponents;
        this.commands = commands;
        this.messages = messages;
    }
}
