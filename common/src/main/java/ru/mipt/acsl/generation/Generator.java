package ru.mipt.acsl.generation;

import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * @author Artem Shein
 */
public interface Generator<C>
{
    /**
     * @throws GenerationException
     */
    void generate();

    @NotNull
    Optional<C> getConfiguration();
}
