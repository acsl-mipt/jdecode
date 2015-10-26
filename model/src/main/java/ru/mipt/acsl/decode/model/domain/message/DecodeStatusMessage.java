package ru.mipt.acsl.decode.model.domain.message;

/**
 * @author Artem Shein
 */
public interface DecodeStatusMessage extends DecodeMessage
{
    default <T> T accept(DecodeMessageVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}
