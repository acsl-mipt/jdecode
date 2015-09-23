package ru.mipt.acsl.decode.model.domain.type;

import org.jetbrains.annotations.NotNull;

/**
 * @author Artem Shein
 */
public interface DecodeTypeVisitor<T, E extends Throwable>
{
    T visit(@NotNull DecodePrimitiveType primitiveType) throws E;

    T visit(@NotNull DecodeNativeType nativeType) throws E;

    T visit(@NotNull DecodeSubType subType) throws E;

    T visit(@NotNull DecodeEnumType enumType) throws E;

    T visit(@NotNull DecodeArrayType arrayType) throws E;

    T visit(@NotNull DecodeStructType structType) throws E;

    T visit(@NotNull DecodeAliasType typeAlias) throws E;
}
