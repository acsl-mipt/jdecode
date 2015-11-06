package ru.mipt.acsl.decode.model.domain.type;

import org.jetbrains.annotations.NotNull;
import ru.mipt.acsl.decode.model.domain.IDecodeName;

import java.util.List;
import java.util.Optional;

/**
 * @author Artem Shein
 */
public interface DecodeGenericType extends DecodeType
{
    @NotNull
    List<Optional<IDecodeName>> getTypeParameters();

    @Override
    default <T> T accept(@NotNull DecodeTypeVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}
