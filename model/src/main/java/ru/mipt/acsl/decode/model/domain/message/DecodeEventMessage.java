package ru.mipt.acsl.decode.model.domain.message;

import org.jetbrains.annotations.NotNull;

/**
 * @author Artem Shein
 */
public interface DecodeEventMessage extends DecodeMessage
{
    default <T> T accept(@NotNull DecodeMessageVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}
