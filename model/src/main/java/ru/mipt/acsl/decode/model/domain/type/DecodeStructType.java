package ru.mipt.acsl.decode.model.domain.type;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author Artem Shein
 */
public interface DecodeStructType extends DecodeType
{
    @NotNull
    List<DecodeStructField> getFields();

    @Override
    default <T> T accept(@NotNull DecodeTypeVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}
