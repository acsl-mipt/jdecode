package ru.mipt.acsl.decode.model.domain.message;

/**
 * @author Artem Shein
 */
public interface DecodeStatusMessage extends DecodeMessage
{
    default <T, E extends Throwable> T accept(DecodeMessageVisitor<T, E> visitor) throws E
    {
        return visitor.visit(this);
    }
}
