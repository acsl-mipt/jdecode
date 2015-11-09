package ru.mipt.acsl.decode.model.domain.impl.type;

import org.jetbrains.annotations.NotNull;
import ru.mipt.acsl.decode.model.domain.*;
import ru.mipt.acsl.decode.model.domain.impl.AbstractDecodeNameNamespaceOptionalInfoAware;
import ru.mipt.acsl.decode.model.domain.message.DecodeMessage;
import ru.mipt.acsl.decode.model.domain.proxy.DecodeMaybeProxy;

import java.util.List;
import java.util.Optional;

/**
 * @author Artem Shein
 */

{
    @NotNull
    private final Optional<Integer> id;
    @NotNull
    private final Optional<DecodeMaybeProxy<DecodeType>> baseType;
    @NotNull
    private final List<DecodeComponentRef> subComponents;
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
    public List<DecodeComponentRef> getSubComponents()
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
    public Optional<String> getInfo()
    {
        return info;
    }

    public static DecodeComponent newInstance
    {
        return new SimpleDecodeComponent(name, namespace, id, baseType, info, subComponents, commands, messages);
    }

    private SimpleDecodeComponent(@NotNull DecodeName name, @NotNull DecodeNamespace namespace,
                                  @NotNull Optional<Integer> id, @NotNull Optional<DecodeMaybeProxy<DecodeType>> baseType,
                                  @NotNull Optional<String> info,
                                  @NotNull List<DecodeComponentRef> subComponents,
                                  @NotNull List<DecodeCommand> commands, @NotNull List<DecodeMessage> messages)
    {
        super(name, namespace, info);
        this.id = id;
        this.baseType = baseType;
        this.subComponents = subComponents;
        this.commands = commands;
        this.messages = messages;
    }
}
