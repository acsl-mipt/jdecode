package ru.mipt.acsl.decode.model.domain.impl;

import ru.mipt.acsl.decode.model.domain.DecodeComponent;
import ru.mipt.acsl.decode.model.domain.DecodeName;
import ru.mipt.acsl.decode.model.domain.message.DecodeEventMessage;
import ru.mipt.acsl.decode.model.domain.message.DecodeMessageParameter;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

/**
 * @author Artem Shein
 */
public class ImmutableDecodeEventMessage extends AbstractImmutableDecodeMessage implements DecodeEventMessage
{
    @NotNull
    public static DecodeEventMessage newInstance(@NotNull DecodeComponent component, @NotNull DecodeName name, int id, @NotNull Optional<String> info,
                                                @NotNull List<DecodeMessageParameter> parameters)
    {
        return new ImmutableDecodeEventMessage(component, name, id, info, parameters);
    }

    @NotNull
    @Override
    public String toString()
    {
        return String.format("%s{name=%s, id=%s, info=%s, parameters=%s}", ImmutableDecodeEventMessage.class.getName(),
                name, id, info, parameters);
    }

    private ImmutableDecodeEventMessage(@NotNull DecodeComponent component, @NotNull DecodeName name, int id,
                                        @NotNull Optional<String> info,
                                        @NotNull List<DecodeMessageParameter> parameters)
    {
        super(component, name, id, info, parameters);
    }
}
