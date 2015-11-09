package ru.mipt.acsl.decode.model.domain.impl.type;

import org.jetbrains.annotations.NotNull;
import ru.mipt.acsl.decode.model.domain.*;
import scala.Option;

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
                                 @NotNull DecodeMaybeProxy<DecodeType> type, @NotNull Option<String> info)
    {
        super(info);
        this.name = name;
        this.namespace = namespace;
        this.type = type;
    }

    @NotNull
    public static DecodeAliasType newInstance(@NotNull DecodeName name, @NotNull DecodeNamespace namespace,
                                              @NotNull DecodeMaybeProxy<DecodeType> type,
                                              @NotNull Option<String> info)
    {
        return new SimpleDecodeAliasType(name, namespace, type, info);
    }

    @NotNull
    @Override
    public Option<DecodeName> optionalName()
    {
        return Option.apply(name);
    }

    @NotNull
    @Override
    public DecodeMaybeProxy<DecodeType> baseType()
    {
        return type;
    }

    @NotNull
    @Override
    public DecodeName name()
    {

        return name;
    }

    @NotNull
    @Override
    public String toString()
    {
        return String.format("%s{name=%s, namespace=%s, type=%s, info=%s}", SimpleDecodeAliasType.class.getName(), name,
                namespace.fqn(), type, info());
    }

    @NotNull
    @Override
    public DecodeNamespace namespace()
    {
        return namespace;
    }

    @Override
    public void namespace_$eq(@NotNull DecodeNamespace namespace)
    {
        this.namespace = namespace;
    }
}
