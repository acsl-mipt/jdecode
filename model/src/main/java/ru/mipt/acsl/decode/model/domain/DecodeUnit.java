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
    default <T, E extends Throwable> T accept(@NotNull DecodeReferenceableVisitor<T, E> visitor) throws E
    {
        return visitor.visit(this);
    }
}
