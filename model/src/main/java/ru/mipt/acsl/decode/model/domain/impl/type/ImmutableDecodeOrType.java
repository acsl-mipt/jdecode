package ru.mipt.acsl.decode.model.domain.impl.type;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import ru.mipt.acsl.decode.model.domain.DecodeNamespace;
import ru.mipt.acsl.decode.model.domain.IDecodeName;
import ru.mipt.acsl.decode.model.domain.impl.DecodeName;
import ru.mipt.acsl.decode.model.domain.type.DecodeGenericType;
import ru.mipt.acsl.decode.model.domain.type.DecodeType;

import java.util.List;
import java.util.Optional;

/**
 * @author Artem Shein
 */
public class ImmutableDecodeOrType extends AbstractDecodeType implements DecodeGenericType
{
    public static final IDecodeName MANGLED_NAME = DecodeName.newFromMangledName("or");
    @NotNull
    private final List<Optional<IDecodeName>> typeParameters = Lists.newArrayList(
            Optional.of(DecodeName.newFromMangledName("L")),
            Optional.of(DecodeName.newFromMangledName("R")));

    public static DecodeGenericType newInstance(@NotNull Optional<IDecodeName> name, @NotNull DecodeNamespace namespace,
                                                @NotNull Optional<String> info)
    {
        return new ImmutableDecodeOrType(name, namespace, info);
    }

    @NotNull
    @Override
    public List<Optional<IDecodeName>> getTypeParameters()
    {
        return typeParameters;
    }

    private ImmutableDecodeOrType(@NotNull Optional<IDecodeName> name,
                                  @NotNull DecodeNamespace namespace,
                                  @NotNull Optional<String> info)
    {
        super(name, namespace, info);
    }
}
