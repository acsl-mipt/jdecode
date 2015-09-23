package ru.mipt.acsl.decode.common;

import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * @author Artem Shein
 */
public final class OptionalUtils
{
    public static <T> Optional<T> castOptionalIfIsInstanceOf(@NotNull Optional<?> object, @NotNull Class<T> cls)
    {
        return object.filter(cls::isInstance).map(cls::cast);
    }

    public static <T> Optional<T> castIfIsInstanceOf(@NotNull Object object, @NotNull Class<T> cls)
    {
        return cls.isInstance(object) ? Optional.of(cls.cast(object)) : Optional.<T>empty();
    }

    public static <T> Optional<T> orElse(@NotNull Optional<T> optional, @NotNull Supplier<Optional<T>> supplier)
    {
        if (optional.isPresent())
        {
            return optional;
        }
        return supplier.get();
    }

    private OptionalUtils()
    {

    }
}
