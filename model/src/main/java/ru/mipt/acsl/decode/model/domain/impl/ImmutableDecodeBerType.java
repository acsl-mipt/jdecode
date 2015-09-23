package ru.mipt.acsl.decode.model.domain.impl;

import org.jetbrains.annotations.NotNull;
import ru.mipt.acsl.decode.model.domain.DecodeName;
import ru.mipt.acsl.decode.model.domain.DecodeNamespace;
import ru.mipt.acsl.decode.model.domain.type.DecodeNativeType;
import ru.mipt.acsl.decode.model.domain.type.DecodeTypeVisitor;

import java.util.Optional;

/**
 * @author Artem Shein
 */
public class ImmutableDecodeBerType extends AbstractDecodeType implements DecodeNativeType
{
    public static final DecodeName MANGLED_NAME = ImmutableDecodeName.newInstanceFromMangledName("ber");

    public static DecodeNativeType newInstance(@NotNull Optional<DecodeName> name,
                                              @NotNull DecodeNamespace namespace,
                                              @NotNull Optional<String> info)
    {
        return new ImmutableDecodeBerType(name, namespace, info);
    }

    private ImmutableDecodeBerType(@NotNull Optional<DecodeName> name,
                                   @NotNull DecodeNamespace namespace,
                                   @NotNull Optional<String> info)
    {
        super(name, namespace, info);
    }

    @Override
    public <T, E extends Throwable> T accept(@NotNull DecodeTypeVisitor<T, E> visitor) throws E
    {
        return visitor.visit(this);
    }
}
