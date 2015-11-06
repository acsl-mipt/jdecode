package ru.mipt.acsl.decode.model.domain.impl.type;

import ru.mipt.acsl.decode.model.domain.IDecodeName;
import ru.mipt.acsl.decode.model.domain.proxy.DecodeMaybeProxy;
import ru.mipt.acsl.decode.model.domain.DecodeNamespace;
import ru.mipt.acsl.decode.model.domain.type.DecodeSubType;
import ru.mipt.acsl.decode.model.domain.type.DecodeType;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * @author Artem Shein
 */
public class SimpleDecodeSubType extends AbstractDecodeType implements DecodeSubType
{
    @NotNull
    private final DecodeMaybeProxy<DecodeType> baseType;

    public static DecodeSubType newInstance(@NotNull Optional<IDecodeName> name,
                                           @NotNull DecodeNamespace namespace,
                                           @NotNull DecodeMaybeProxy<DecodeType> baseType,
                                           @NotNull Optional<String> info)
    {
        return new SimpleDecodeSubType(name, namespace, baseType, info);
    }

    private SimpleDecodeSubType(@NotNull Optional<IDecodeName> name, @NotNull DecodeNamespace namespace,
                                @NotNull DecodeMaybeProxy<DecodeType> baseType,
                                @NotNull Optional<String> info)
    {
        super(name, namespace, info);
        this.baseType = baseType;
    }

    @NotNull
    @Override
    public DecodeMaybeProxy<DecodeType> getBaseType()
    {
        return baseType;
    }

    @NotNull
    @Override
    public String toString()
    {
        return String.format("%s{name=%s, namespace=%s, baseType=%s, info=%s}", SimpleDecodeSubType.class.getName(),
                name, namespace.getFqn(), baseType, info);
    }
}
