package ru.mipt.acsl.generator.java.ast;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * @author Artem Shein
 */
public class JavaAssignStatement implements JavaStatement
{
    @NotNull
    private final JavaVarExpr var;
    @NotNull
    private final JavaExpr expr;

    public JavaAssignStatement(@NotNull JavaVarExpr var, @NotNull JavaExpr expr)
    {
        this.var = var;
        this.expr = expr;
    }

    @Override
    public void generate(@NotNull JavaGeneratorState state, @NotNull Appendable appendable) throws IOException
    {
        var.generate(state, appendable);
        appendable.append(" = ");
        expr.generate(state, appendable);
    }
}
