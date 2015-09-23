package ru.mipt.acsl.decode.model.domain.impl;

import ru.mipt.acsl.decode.model.domain.DecodeName;
import ru.mipt.acsl.decode.model.domain.DecodeNamespace;
import ru.mipt.acsl.decode.model.domain.proxy.DecodeMaybeProxy;
import ru.mipt.acsl.decode.model.domain.type.DecodeEnumConstant;
import ru.mipt.acsl.decode.model.domain.type.DecodeType;
import com.google.common.collect.ImmutableSet;
import ru.mipt.acsl.decode.model.domain.type.DecodeEnumType;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.Set;

/**
 * @author Artem Shein
 */
public class SimpleDecodeEnumType extends AbstractDecodeType implements DecodeEnumType
{
    @NotNull
    private final DecodeMaybeProxy<DecodeType> baseType;
    @NotNull
    private final Set<DecodeEnumConstant> constants;

    public static DecodeEnumType newInstance(@NotNull Optional<DecodeName> name,
                                            @NotNull DecodeNamespace namespace,
                                            @NotNull DecodeMaybeProxy<DecodeType> baseType,
                                            @NotNull Optional<String> info,
                                            @NotNull Set<DecodeEnumConstant> constants)
    {
        return new SimpleDecodeEnumType(name, namespace, baseType, info, constants);
    }

    private SimpleDecodeEnumType(@NotNull Optional<DecodeName> name,
                                 @NotNull DecodeNamespace namespace,
                                 @NotNull DecodeMaybeProxy<DecodeType> baseType,
                                 @NotNull Optional<String> info,
                                 @NotNull Set<DecodeEnumConstant> constants)
    {
        super(name, namespace, info);
        this.baseType = baseType;
        this.constants = ImmutableSet.copyOf(constants);
    }

    @NotNull
    @Override
    public DecodeMaybeProxy<DecodeType> getBaseType()
    {
        return baseType;
    }

    @NotNull
    @Override
    public Set<DecodeEnumConstant> getConstants()
    {
        return constants;
    }

    @NotNull
    @Override
    public String toString()
    {
        return String.format("%s{name=%s, namespace=%s, baseType=%s, info=%s, constants=%s}",
                SimpleDecodeEnumType.class.getName(), name, namespace, baseType, info, constants);
    }
}
