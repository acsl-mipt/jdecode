package ru.mipt.acsl.decode.model.domain.impl;

import ru.mipt.acsl.decode.model.domain.DecodeComponent;
import ru.mipt.acsl.decode.model.domain.message.DecodeMessageParameter;
import ru.mipt.acsl.decode.model.domain.message.DecodeStatusMessage;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

/**
 * @author Artem Shein
 */
public class ImmutableDecodeStatusMessage extends AbstractImmutableDecodeMessage implements DecodeStatusMessage
{
    @NotNull
    public static DecodeStatusMessage newInstance(@NotNull DecodeComponent component, @NotNull DecodeNameImpl name,
                                                  @NotNull Optional<Integer> id, @NotNull Optional<String> info,
                                                  @NotNull List<DecodeMessageParameter> parameters)
    {
        return new ImmutableDecodeStatusMessage(component, name, id, info, parameters);
    }

    private ImmutableDecodeStatusMessage(@NotNull DecodeComponent component, @NotNull DecodeNameImpl name,
                                         @NotNull Optional<Integer> id,
                                         @NotNull Optional<String> info,
                                         @NotNull List<DecodeMessageParameter> parameters)
    {
        super(component, name, id, info, parameters);
    }

    @NotNull
    @Override
    public String toString()
    {
        return String.format("%s{name=%s, id=%s, info=%s, parameters=%s}",
                ImmutableDecodeStatusMessage.class.getName(), name, id, info, parameters);
    }
}
