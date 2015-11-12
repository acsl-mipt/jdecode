package ru.mipt.acsl;

import org.jetbrains.annotations.NotNull;
import scala.Option;

import java.util.Optional;

/**
 * @author Artem Shein
 */
public final class ScalaToJava
{
    private ScalaToJava()
    {

    }

    @NotNull
    public static <T> Optional<T> asOptional(@NotNull Option<T> option)
    {
        return option.isDefined() ? Optional.of(option.get()) : Optional.empty();
    }
}
