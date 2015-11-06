package ru.mipt.acsl.decode.model.domain.type;

import org.jetbrains.annotations.NotNull;
import ru.mipt.acsl.decode.model.domain.DecodeName;

import java.util.List;
import java.util.Optional;

/**
 * @author Artem Shein
 */
public interface DecodeGenericType extends DecodeType
{
    @NotNull
    List<Optional<DecodeName>> getTypeParameters();

    @Override
    default <T> T accept(@NotNull DecodeTypeVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}
