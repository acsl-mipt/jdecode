package ru.mipt.acsl.decode.model.domain.message;

import org.jetbrains.annotations.NotNull;

/**
 * @author Artem Shein
 */
public interface DecodeEventMessage extends DecodeMessage
{
    default <T, E extends Throwable> T accept(@NotNull DecodeMessageVisitor<T, E> visitor) throws E
    {
        return visitor.visit(this);
    }
}
