package ru.mipt.acsl.decode.model.domain.type;

import org.jetbrains.annotations.NotNull;

/**
 * @author Artem Shein
 */
public interface DecodePrimitiveType extends DecodeType
{
    long getBitLength();

    @NotNull
    TypeKind getKind();

    @Override
    default <T> T accept(@NotNull DecodeTypeVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}
