package ru.mipt.acsl.decode.model.domain.message;

import org.jetbrains.annotations.NotNull;

/**
 * @author Artem Shein
 */
public interface DecodeMessageVisitor<T>
{
    T visit(@NotNull DecodeEventMessage eventMessage);
    T visit(@NotNull DecodeStatusMessage statusMessage);
}
