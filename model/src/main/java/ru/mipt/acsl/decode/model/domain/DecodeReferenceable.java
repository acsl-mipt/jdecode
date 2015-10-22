package ru.mipt.acsl.decode.model.domain;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Artem Shein
 */
public interface DecodeReferenceable extends DecodeNameAware
{
    @Nullable
    <T> T accept(@NotNull DecodeReferenceableVisitor<T> visitor);
}
