package ru.mipt.acsl.decode.model.domain.impl.type;

import org.jetbrains.annotations.NotNull;
import ru.mipt.acsl.decode.model.domain.DecodeNamespace;
import ru.mipt.acsl.decode.model.domain.DecodeName;
import ru.mipt.acsl.decode.model.domain.proxy.DecodeMaybeProxy;
import ru.mipt.acsl.decode.model.domain.type.DecodeGenericType;
import ru.mipt.acsl.decode.model.domain.type.DecodeGenericTypeSpecialized;
import ru.mipt.acsl.decode.model.domain.type.DecodeType;

import java.util.List;
import java.util.Optional;

/**
 * @author Artem Shein
 */
public class ImmutableDecodeGenericTypeSpecialized extends AbstractDecodeType implements DecodeGenericTypeSpecialized
{
    @NotNull
    private DecodeMaybeProxy<DecodeGenericType> genericType;
    @NotNull
    private List<Optional<DecodeMaybeProxy<DecodeType>>> genericTypeArguments;

    public static DecodeGenericTypeSpecialized newInstance(@NotNull Optional<DecodeName> name,
                                                           @NotNull DecodeNamespace namespace,
                                                           @NotNull Optional<String> info,
                                                           @NotNull DecodeMaybeProxy<DecodeGenericType> genericType,
                                                           @NotNull
                                                           List<Optional<DecodeMaybeProxy<DecodeType>>> genericTypeArguments)
    {
        return new ImmutableDecodeGenericTypeSpecialized(name, namespace, info, genericType, genericTypeArguments);
    }

    @NotNull
    @Override
    public DecodeMaybeProxy<DecodeGenericType> getGenericType()
    {
        return genericType;
    }

    @NotNull
    @Override
    public List<Optional<DecodeMaybeProxy<DecodeType>>> getGenericTypeArguments()
    {
        return genericTypeArguments;
    }

    private ImmutableDecodeGenericTypeSpecialized(@NotNull Optional<DecodeName> name,
                                                  @NotNull DecodeNamespace namespace,
                                                  @NotNull Optional<String> info,
                                                  @NotNull DecodeMaybeProxy<DecodeGenericType> genericType,
                                                  @NotNull
                                                  List<Optional<DecodeMaybeProxy<DecodeType>>> genericTypeArguments)
    {
        super(name, namespace, info);
        this.genericType = genericType;
        this.genericTypeArguments = genericTypeArguments;
    }
}
