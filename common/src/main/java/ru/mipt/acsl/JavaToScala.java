package ru.mipt.acsl;

import org.jetbrains.annotations.NotNull;
import scala.Option;

import java.util.Optional;

/**
 * @author Artem Shein
 */
public final class JavaToScala
{
    @NotNull
    public static <T> Option<T> asOption(@NotNull Optional<T> optional)
    {
        return Option.apply(optional.orElse(null));
    }

    private JavaToScala()
    {

    }
}
