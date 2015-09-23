package ru.mipt.acsl.generator.java.ast;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.mipt.acsl.generation.Generatable;

import java.io.IOException;

/**
 * @author Artem Shein
 */
public interface JavaAstElement extends Generatable<JavaGeneratorState>
{
    @Override
    void generate(@NotNull JavaGeneratorState state, @NotNull Appendable appendable) throws IOException;
}
