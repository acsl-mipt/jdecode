package ru.mipt.acsl.decode.model.domain;

import ru.mipt.acsl.decode.model.domain.impl.ImmutableDecodeName;
import org.jetbrains.annotations.NotNull;

/**
 * @author Artem Shein
 */
public final class DecodeConstants
{
    @NotNull
    public static final DecodeName SYSTEM_NAMESPACE_NAME = ImmutableDecodeName.newInstanceFromMangledName("decode");

    private DecodeConstants()
    {
    }
}
