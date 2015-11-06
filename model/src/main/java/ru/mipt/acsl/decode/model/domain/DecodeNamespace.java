package ru.mipt.acsl.decode.model.domain;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.Nullable;
import ru.mipt.acsl.decode.model.domain.impl.ImmutableDecodeFqn;
import ru.mipt.acsl.decode.model.domain.type.DecodeType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @author Artem Shein
 */
public interface DecodeNamespace extends DecodeReferenceable, DecodeNameAware
{
    @NotNull
    String asString();

    @NotNull
    List<DecodeUnit> getUnits();

    @NotNull
    List<DecodeType> getTypes();

    void setTypes(@NotNull List<DecodeType> types);

    @NotNull
    List<DecodeNamespace> getSubNamespaces();

    @NotNull
    Optional<DecodeNamespace> getParent();

    @NotNull
    List<DecodeComponent> getComponents();

    default <T> T accept(@NotNull DecodeReferenceableVisitor<T> visitor)
    {
        return visitor.visit(this);
    }

    @NotNull
    List<DecodeLanguage> getLanguages();

    @NotNull
    default DecodeFqn getFqn()
    {
        List<IDecodeName> parts = new ArrayList<>();
        DecodeNamespace currentNamespace = this;
        while (currentNamespace.getParent().isPresent())
        {
            parts.add(currentNamespace.getName());
            currentNamespace = currentNamespace.getParent().get();
        }
        parts.add(currentNamespace.getName());
        return ImmutableDecodeFqn.newInstance(Lists.reverse(parts));
    }

    void setParent(@Nullable DecodeNamespace parent);

    @NotNull
    default DecodeElement getRootNamespace()
    {
        return getParent().map(DecodeNamespace::getRootNamespace).orElse(this);
    }
}
