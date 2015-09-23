package ru.mipt.acsl.decode.model.domain.impl;

import ru.mipt.acsl.decode.model.domain.DecodeComponent;
import ru.mipt.acsl.decode.model.domain.DecodeName;
import ru.mipt.acsl.decode.model.domain.message.DecodeDynamicStatusMessage;
import ru.mipt.acsl.decode.model.domain.message.DecodeMessageParameter;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

/**
 * @author Artem Shein
 */
public class ImmutableDecodeDynamicStatusMessage extends AbstractImmutableDecodeMessage
        implements DecodeDynamicStatusMessage
{
    @NotNull
    public static DecodeDynamicStatusMessage newInstance(@NotNull DecodeComponent component, @NotNull DecodeName name,
                                                        int id, @NotNull Optional<String> info,
                                                        @NotNull List<DecodeMessageParameter> parameters)
    {
        return new ImmutableDecodeDynamicStatusMessage(component, name, id, info, parameters);
    }


    @NotNull
    @Override
    public String toString()
    {
        return String.format("%s{name=%s, id=%s, parameters=%s, info=%s}",
                ImmutableDecodeDynamicStatusMessage.class.getName(), name, id, parameters, info);
    }

    private ImmutableDecodeDynamicStatusMessage(@NotNull DecodeComponent component, @NotNull DecodeName name, int id,
                                                @NotNull Optional<String> info,
                                                @NotNull List<DecodeMessageParameter> parameters)
    {
        super(component, name, id, info, parameters);
    }
}
