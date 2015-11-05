package ru.mipt.acsl.decode.model.domain.type;

import org.jetbrains.annotations.NotNull;

/**
 * @author Artem Shein
 */
public interface DecodeTypeVisitor<T>
{
    T visit(@NotNull DecodePrimitiveType primitiveType);
    T visit(@NotNull DecodeNativeType nativeType);
    T visit(@NotNull DecodeSubType subType);
    T visit(@NotNull DecodeEnumType enumType);
    T visit(@NotNull DecodeArrayType arrayType);
    T visit(@NotNull DecodeStructType structType);
    T visit(@NotNull DecodeAliasType typeAlias);
    T visit(@NotNull DecodeGenericType genericType);
    T visit(@NotNull DecodeGenericTypeSpecialized genericTypeSpecialized);
}
