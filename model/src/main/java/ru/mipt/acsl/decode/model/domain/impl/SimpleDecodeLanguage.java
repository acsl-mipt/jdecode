package ru.mipt.acsl.decode.model.domain.impl;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.mipt.acsl.decode.model.domain.*;

import java.util.Optional;

/**
 * @author Artem Shein
 */
public class SimpleDecodeLanguage extends AbstractDecodeNameNamespaceOptionalInfoAware implements DecodeLanguage
{
    private final boolean isDefault;

    private SimpleDecodeLanguage(@NotNull DecodeNameImpl name, @NotNull DecodeNamespace namespace, boolean isDefault,
                                 @NotNull Optional<String> info)
    {
        super(name, namespace, info);
        this.isDefault = isDefault;
    }

    public static DecodeLanguage newInstance(@NotNull DecodeNameImpl name, @NotNull DecodeNamespace namespace,
                                             boolean isDefault,
                                             @NotNull Optional<String> info)
    {
        return new SimpleDecodeLanguage(name, namespace, isDefault, info);
    }

    @Nullable
    @Override
    public <T> T accept(@NotNull DecodeReferenceableVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}
