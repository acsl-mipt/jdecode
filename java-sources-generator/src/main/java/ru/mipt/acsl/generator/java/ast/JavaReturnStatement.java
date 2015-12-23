package ru.mipt.acsl.generator.java.ast;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * @author Artem Shein
 */
public class JavaReturnStatement implements JavaStatement
{
    @NotNull
    private final JavaExpr expr;

    public JavaReturnStatement(@NotNull JavaExpr expr)
    {
        this.expr = expr;
    }

    @Override
    public void generate(@NotNull JavaGeneratorState state)
    {
        state.append("return ");
        expr.generate(state);
    }
}
