package ru.mipt.acsl.generator.java.ast;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * @author Artem Shein
 */
public class JavaStringExpr implements JavaExpr
{
    @NotNull
    private final String str;

    public JavaStringExpr(@NotNull String str)
    {
        this.str = str;
    }

    @Override
    public void generate(@NotNull JavaGeneratorState state)
    {
        state.append("\"").append(str).append("\"");
    }
}
