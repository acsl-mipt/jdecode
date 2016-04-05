package ru.mipt.acsl.generator.java.ast;

import org.jetbrains.annotations.NotNull;

/**
 * @author Artem Shein
 */
public interface JavaAstElement
{
    void generate(@NotNull JavaGeneratorState state);
}
