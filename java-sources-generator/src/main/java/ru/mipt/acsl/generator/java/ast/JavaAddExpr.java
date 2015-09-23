package ru.mipt.acsl.generator.java.ast;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;

/**
 * @author Artem Shein
 */
public class JavaAddExpr implements JavaExpr
{
    @NotNull
    private final List<JavaExpr> exprs;

    public JavaAddExpr(@NotNull JavaExpr... exprs)
    {
        Preconditions.checkArgument(exprs.length > 1);
        this.exprs = Lists.newArrayList(exprs);
    }

    @Override
    public void generate(@NotNull JavaGeneratorState state, @NotNull Appendable appendable) throws IOException
    {
        boolean isFirst = true;
        for (JavaExpr expr : exprs)
        {
            if (isFirst)
            {
                isFirst = false;
            }
            else
            {
                appendable.append(" + ");
            }
            expr.generate(state, appendable);
        }
    }
}
