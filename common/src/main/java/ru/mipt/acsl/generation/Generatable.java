package ru.mipt.acsl.generation;

import org.jetbrains.annotations.NotNull;

/**
 * @author Artem Shein
 */
@FunctionalInterface
public interface Generatable<S>
{
    void generate(@NotNull S generatorState);
}
