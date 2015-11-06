package ru.mipt.acsl.decode.model.domain.impl.type;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import ru.mipt.acsl.decode.model.domain.DecodeNamespace;
import ru.mipt.acsl.decode.model.domain.DecodeName;
import ru.mipt.acsl.decode.model.domain.impl.DecodeNameImpl;
import ru.mipt.acsl.decode.model.domain.type.DecodeGenericType;

import java.util.List;
import java.util.Optional;

/**
 * @author Artem Shein
 */
public class ImmutableDecodeOrType extends AbstractDecodeType implements DecodeGenericType
{
    public static final DecodeName MANGLED_NAME = DecodeNameImpl.newFromMangledName("or");
    @NotNull
    private final List<Optional<DecodeName>> typeParameters = Lists.newArrayList(
            Optional.of(DecodeNameImpl.newFromMangledName("L")),
            Optional.of(DecodeNameImpl.newFromMangledName("R")));

    public static DecodeGenericType newInstance(@NotNull Optional<DecodeName> name, @NotNull DecodeNamespace namespace,
                                                @NotNull Optional<String> info)
    {
        return new ImmutableDecodeOrType(name, namespace, info);
    }

    @NotNull
    @Override
    public List<Optional<DecodeName>> getTypeParameters()
    {
        return typeParameters;
    }

    private ImmutableDecodeOrType(@NotNull Optional<DecodeName> name,
                                  @NotNull DecodeNamespace namespace,
                                  @NotNull Optional<String> info)
    {
        super(name, namespace, info);
    }
}
