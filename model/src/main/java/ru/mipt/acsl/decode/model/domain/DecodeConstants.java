package ru.mipt.acsl.decode.model.domain;

import org.jetbrains.annotations.NotNull;
import ru.mipt.acsl.decode.model.domain.impl.type.DecodeFqnImpl;

/**
 * @author Artem Shein
 */
public final class DecodeConstants
{
    @NotNull
    public static final DecodeFqn SYSTEM_NAMESPACE_FQN = DecodeFqnImpl.newFromSource("decode");

    private DecodeConstants()
    {
    }
}
