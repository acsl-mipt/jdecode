package ru.mipt.acsl.generation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

/**
 * @author Artem Shein
 */
public interface StatelessGeneratable extends Generatable<Void>
{
    @Override
    default void generate(@Nullable Void ignored, @NotNull Appendable appendable) throws IOException
    {
        generate(appendable);
    }

    void generate(@NotNull Appendable appendable) throws IOException;
}
