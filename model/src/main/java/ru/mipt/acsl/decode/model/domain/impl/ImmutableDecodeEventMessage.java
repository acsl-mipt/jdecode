package ru.mipt.acsl.decode.model.domain.impl;

import ru.mipt.acsl.decode.model.domain.DecodeComponent;
import ru.mipt.acsl.decode.model.domain.DecodeName;
import ru.mipt.acsl.decode.model.domain.message.DecodeEventMessage;
import ru.mipt.acsl.decode.model.domain.message.DecodeMessageParameter;
import org.jetbrains.annotations.NotNull;
import ru.mipt.acsl.decode.model.domain.proxy.DecodeMaybeProxy;
import ru.mipt.acsl.decode.model.domain.type.DecodeType;

import java.util.List;
import java.util.Optional;

/**
 * @author Artem Shein
 */
public class ImmutableDecodeEventMessage extends AbstractImmutableDecodeMessage implements DecodeEventMessage
{
    @NotNull
    protected final DecodeMaybeProxy<DecodeType> eventType;

    @NotNull
    public static DecodeEventMessage newInstance(@NotNull DecodeComponent component, @NotNull DecodeName name,
                                                 @NotNull Optional<Integer> id, @NotNull Optional<String> info,
                                                 @NotNull List<DecodeMessageParameter> parameters,
                                                 @NotNull DecodeMaybeProxy<DecodeType> eventType)
    {
        return new ImmutableDecodeEventMessage(component, name, id, info, parameters, eventType);
    }

    @NotNull
    @Override
    public String toString()
    {
        return String.format("%s{name=%s, id=%s, info=%s, parameters=%s, eventType=%s}", ImmutableDecodeEventMessage.class.getName(),
                name, id, info, parameters, eventType);
    }

    private ImmutableDecodeEventMessage(@NotNull DecodeComponent component, @NotNull DecodeName name,
                                        @NotNull Optional<Integer> id,
                                        @NotNull Optional<String> info,
                                        @NotNull List<DecodeMessageParameter> parameters,
                                        @NotNull DecodeMaybeProxy<DecodeType> eventType)
    {
        super(component, name, id, info, parameters);
        this.eventType = eventType;
    }
}
