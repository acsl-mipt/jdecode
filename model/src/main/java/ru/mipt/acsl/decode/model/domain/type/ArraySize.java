package ru.mipt.acsl.decode.model.domain.type;

/**
 * @author Artem Shein
 */
public interface ArraySize
{
    long getMinLength();
    long getMaxLength();
}
