package ru.mipt.acsl.decode.model.domain.type;

import ru.mipt.acsl.decode.model.domain.proxy.DecodeMaybeProxy;
import org.jetbrains.annotations.NotNull;

/**
 * @author Artem Shein
 */
public interface DecodeSubType extends DecodeType
{
    @NotNull
    DecodeMaybeProxy<DecodeType> getBaseType();

    @Override
    default <T, E extends Throwable> T accept(@NotNull DecodeTypeVisitor<T, E> visitor) throws E
    {
        return visitor.visit(this);
    }
}
