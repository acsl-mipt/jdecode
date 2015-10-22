package ru.mipt.acsl.decode.model.domain;

import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * @author Artem Shein
 */
public interface DecodeUnit extends DecodeNameAware, DecodeOptionalInfoAware, DecodeReferenceable, DecodeNamespaceAware
{
    @NotNull
    Optional<String> getDisplay();

    @Override
    default <T> T accept(@NotNull DecodeReferenceableVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}
