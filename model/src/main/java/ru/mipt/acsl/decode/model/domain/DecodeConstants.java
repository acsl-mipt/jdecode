package ru.mipt.acsl.decode.model.domain;

import ru.mipt.acsl.decode.model.domain.impl.ImmutableDecodeFqn;
import ru.mipt.acsl.decode.model.domain.impl.ImmutableDecodeName;
import org.jetbrains.annotations.NotNull;

/**
 * @author Artem Shein
 */
public final class DecodeConstants
{
    @NotNull
    public static final DecodeFqn SYSTEM_NAMESPACE_FQN = ImmutableDecodeFqn.newInstanceFromSource("decode");

    private DecodeConstants()
    {
    }
}
