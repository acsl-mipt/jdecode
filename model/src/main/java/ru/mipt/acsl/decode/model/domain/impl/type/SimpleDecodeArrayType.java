package ru.mipt.acsl.decode.model.domain.impl.type;

import ru.mipt.acsl.decode.model.domain.DecodeNamespace;
import ru.mipt.acsl.decode.model.domain.DecodeName;
import ru.mipt.acsl.decode.model.domain.proxy.DecodeMaybeProxy;
import ru.mipt.acsl.decode.model.domain.type.ArraySize;
import ru.mipt.acsl.decode.model.domain.type.DecodeArrayType;
import ru.mipt.acsl.decode.model.domain.type.DecodeType;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * @author Artem Shein
 */
public class SimpleDecodeArrayType extends AbstractDecodeType implements DecodeArrayType
{
    @NotNull
    private final DecodeMaybeProxy<DecodeType> baseType;
    @NotNull
    private final ArraySize size;

    public static DecodeArrayType newInstance(@NotNull Optional<DecodeName> name, @NotNull DecodeNamespace namespace,
                                              @NotNull DecodeMaybeProxy<DecodeType> baseType,
                                              @NotNull Optional<String> info,
                                              @NotNull ArraySize size)
    {
        return new SimpleDecodeArrayType(name, namespace, baseType, info, size);
    }

    private SimpleDecodeArrayType(@NotNull Optional<DecodeName> name, @NotNull DecodeNamespace namespace,
                                  @NotNull DecodeMaybeProxy<DecodeType> baseType,
                                  @NotNull Optional<String> info, @NotNull ArraySize size)
    {
        super(name, namespace, info);
        this.baseType = baseType;
        this.size = size;
    }

    @NotNull
    @Override
    public ArraySize getSize()
    {
        return size;
    }

    @NotNull
    @Override
    public DecodeMaybeProxy<DecodeType> getBaseType()
    {
        return baseType;
    }

    @Override
    public String toString()
    {
        return String.format("%s{name=%s, namespace=%s, baseType=%s, size=%s, info=%s}",
                SimpleDecodeArrayType.class.getName(), name, namespace.getFqn(), baseType, size, info);
    }

}
