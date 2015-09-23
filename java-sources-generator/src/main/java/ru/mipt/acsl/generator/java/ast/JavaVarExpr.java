package ru.mipt.acsl.generator.java.ast;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * @author Artem Shein
 */
public class JavaVarExpr implements JavaExpr
{
    @NotNull
    private final String var;

    public JavaVarExpr(@NotNull String var)
    {
        this.var = var;
    }

    @Override
    public void generate(@NotNull JavaGeneratorState state, @NotNull Appendable appendable) throws IOException
    {
        appendable.append(var);
    }
}
