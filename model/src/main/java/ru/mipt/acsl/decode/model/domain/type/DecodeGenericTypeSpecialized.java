package ru.mipt.acsl.decode.model.domain.type;

import org.jetbrains.annotations.NotNull;
import ru.mipt.acsl.decode.model.domain.proxy.DecodeMaybeProxy;

import java.util.List;
import java.util.Optional;

/**
 * @author Artem Shein
 */
public interface DecodeGenericTypeSpecialized extends DecodeType
{
    @NotNull
    DecodeMaybeProxy<DecodeGenericType> getGenericType();

    @NotNull
    List<Optional<DecodeMaybeProxy<DecodeType>>> getGenericTypeArguments();

    @Override
    default <T> T accept(@NotNull DecodeTypeVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}
