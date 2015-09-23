package ru.mipt.acsl.generator.java.ast;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * @author Artem Shein
 */
public class JavaLongExpr implements JavaExpr
{
    private final long value;

    public JavaLongExpr(long value)
    {
        this.value = value;
    }

    @Override
    public void generate(@NotNull JavaGeneratorState state, @NotNull Appendable appendable) throws IOException
    {
        appendable.append(Long.toString(value));
    }
}
