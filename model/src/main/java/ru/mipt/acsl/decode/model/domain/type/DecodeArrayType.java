package ru.mipt.acsl.decode.model.domain.type;

import ru.mipt.acsl.decode.model.domain.proxy.DecodeMaybeProxy;
import org.jetbrains.annotations.NotNull;

/**
 * @author Artem Shein
 */
public interface DecodeArrayType extends DecodeType
{
    @NotNull
    ArraySize getSize();

    @NotNull
    DecodeMaybeProxy<DecodeType> getBaseType();

    default boolean isFixedSize()
    {
        ArraySize size = getSize();
        long maxLength = size.getMaxLength();
        return size.getMinLength() == maxLength && maxLength != 0;
    }

    @Override
    default <T> T accept(@NotNull DecodeTypeVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}
