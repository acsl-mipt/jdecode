package ru.mipt.acsl.decode.model.domain.impl.type;

import ru.mipt.acsl.decode.model.domain.DecodeName;
import ru.mipt.acsl.decode.model.domain.DecodeNamespace;
import ru.mipt.acsl.decode.model.domain.impl.AbstractDecodeOptionalInfoAware;
import ru.mipt.acsl.decode.model.domain.proxy.DecodeMaybeProxy;
import ru.mipt.acsl.decode.model.domain.type.DecodeAliasType;
import ru.mipt.acsl.decode.model.domain.type.DecodeType;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * @author Artem Shein
 */
public class SimpleDecodeAliasType extends AbstractDecodeOptionalInfoAware implements DecodeAliasType
{
    @NotNull
    private final DecodeName name;
    @NotNull
    private final DecodeMaybeProxy<DecodeType> type;
    @NotNull
    private DecodeNamespace namespace;

    public SimpleDecodeAliasType(@NotNull DecodeName name, @NotNull DecodeNamespace namespace,
                                 @NotNull DecodeMaybeProxy<DecodeType> type, @NotNull Optional<String> info)
    {
        super(info);
        this.name = name;
        this.namespace = namespace;
        this.type = type;
    }

    @NotNull
    public static DecodeAliasType newInstance(@NotNull DecodeName name, @NotNull DecodeNamespace namespace,
                                             @NotNull DecodeMaybeProxy<DecodeType> type,
                                             @NotNull Optional<String> info)
    {
        return new SimpleDecodeAliasType(name, namespace, type, info);
    }

    @NotNull
    @Override
    public Optional<DecodeName> getOptionalName()
    {
        return Optional.of(name);
    }

    @NotNull
    @Override
    public DecodeMaybeProxy<DecodeType> getType()
    {
        return type;
    }

    @NotNull
    @Override
    public DecodeName getName()
    {

        return name;
    }

    @NotNull
    @Override
    public String toString()
    {
        return String.format("%s{name=%s, namespace=%s, type=%s, info=%s}", SimpleDecodeAliasType.class.getName(), name,
                namespace.getFqn(), type, info);
    }

    @NotNull
    @Override
    public DecodeNamespace getNamespace()
    {
        return namespace;
    }

    @Override
    public void setNamespace(@NotNull DecodeNamespace namespace)
    {
        this.namespace = namespace;
    }
}
