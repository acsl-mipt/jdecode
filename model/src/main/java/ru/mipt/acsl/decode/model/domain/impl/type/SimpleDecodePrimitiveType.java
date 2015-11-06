package ru.mipt.acsl.decode.model.domain.impl.type;

import ru.mipt.acsl.decode.model.domain.DecodeName;
import ru.mipt.acsl.decode.model.domain.type.DecodeType;
import ru.mipt.acsl.decode.model.domain.DecodeNamespace;
import ru.mipt.acsl.decode.model.domain.type.DecodePrimitiveType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * @author Artem Shein
 */
public class SimpleDecodePrimitiveType extends AbstractDecodeType implements DecodePrimitiveType
{
    @NotNull
    private final DecodeType.TypeKind kind;

    private final long bitLength;

    public static DecodePrimitiveType newInstance(@NotNull Optional<DecodeName> name, @NotNull DecodeNamespace namespace,
                                                  @NotNull DecodeType.TypeKind kind, long bitLength,
                                                  @NotNull Optional<String> info)
    {
        return new SimpleDecodePrimitiveType(name, namespace, kind, bitLength, info);
    }

    public static DecodePrimitiveType newInstance(@NotNull Optional<DecodeName> name, @NotNull DecodeNamespace namespace,
                                                  @NotNull DecodeType.TypeKind kind, long bitLength,
                                                  @Nullable String info)
    {
        return new SimpleDecodePrimitiveType(name, namespace, kind, bitLength, Optional.ofNullable(info));
    }

    private SimpleDecodePrimitiveType(@NotNull Optional<DecodeName> name, @NotNull DecodeNamespace namespace,
                                      @NotNull DecodeType.TypeKind kind, long bitLength,
                                      @NotNull Optional<String> info)
    {
        super(name, namespace, info);
        this.kind = kind;
        this.bitLength = bitLength;
    }

    @Override
    public long getBitLength()
    {
        return bitLength;
    }

    @NotNull
    @Override
    public DecodeType.TypeKind getKind()
    {
        return kind;
    }

    @NotNull
    @Override
    public String toString()
    {
        return String.format("%s{name=%s, namespace=%s, kind=%s, bitLength=%s, info=%s}",
                SimpleDecodePrimitiveType.class.getName(), name, namespace.getFqn(), kind.getName(), bitLength, info);
    }
}
