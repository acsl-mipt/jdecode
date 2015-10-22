package ru.mipt.acsl.decode.model.domain.type;

import ru.mipt.acsl.decode.model.domain.proxy.DecodeMaybeProxy;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * @author Artem Shein
 */
public interface DecodeEnumType extends DecodeType
{
    @NotNull
    DecodeMaybeProxy<DecodeType> getBaseType();

    @NotNull
    Set<DecodeEnumConstant> getConstants();

    @Override
    default <T> T accept(@NotNull DecodeTypeVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}
