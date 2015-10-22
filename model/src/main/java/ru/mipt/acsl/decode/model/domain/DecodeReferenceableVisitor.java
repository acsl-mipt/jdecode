package ru.mipt.acsl.decode.model.domain;

import org.jetbrains.annotations.NotNull;
import ru.mipt.acsl.decode.model.domain.type.DecodeType;

/**
 * @author Artem Shein
 */
public interface DecodeReferenceableVisitor<T>
{
    T visit(@NotNull DecodeNamespace namespace);
    T visit(@NotNull DecodeType type);
    T visit(@NotNull DecodeComponent component);
    T visit(@NotNull DecodeUnit unit);
    T visit(@NotNull DecodeLanguage language);
}
