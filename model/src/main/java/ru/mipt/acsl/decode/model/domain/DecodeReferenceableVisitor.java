package ru.mipt.acsl.decode.model.domain;

import ru.mipt.acsl.decode.model.domain.type.DecodeType;
import org.jetbrains.annotations.NotNull;

/**
 * @author Artem Shein
 */
public interface DecodeReferenceableVisitor<T, E extends Throwable>
{
    T visit(@NotNull DecodeNamespace namespace) throws E;
    T visit(@NotNull DecodeType type) throws E;
    T visit(@NotNull DecodeComponent component) throws E;
    T visit(@NotNull DecodeUnit unit) throws E;
}
