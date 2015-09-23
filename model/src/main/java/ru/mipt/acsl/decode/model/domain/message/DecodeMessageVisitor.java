package ru.mipt.acsl.decode.model.domain.message;

import org.jetbrains.annotations.NotNull;

/**
 * @author Artem Shein
 */
public interface DecodeMessageVisitor<T, E extends Throwable>
{
    T visit(@NotNull DecodeEventMessage eventMessage) throws E;
    T visit(@NotNull DecodeStatusMessage statusMessage) throws E;
    T visit(@NotNull DecodeDynamicStatusMessage dynamicStatusMessage) throws E;
}
