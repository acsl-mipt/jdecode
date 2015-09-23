package ru.mipt.acsl.generator.java.ast;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;

/**
 * @author Artem Shein
 */
public class JavaSuperCallStatement implements JavaStatement
{
    private final List<JavaExpr> arguments;

    public JavaSuperCallStatement(@NotNull JavaExpr... exprs)
    {
        this.arguments = Lists.newArrayList(exprs);
    }

    @Override
    public void generate(@NotNull JavaGeneratorState state, @NotNull Appendable appendable) throws IOException
    {
        appendable.append("super(");
        if (!arguments.isEmpty())
        {
            boolean isFirst = true;
            for (JavaExpr expr : arguments)
            {
                if (isFirst)
                {
                    isFirst = false;
                }
                else
                {
                    appendable.append(", ");
                }
                expr.generate(state, appendable);
            }
        }
        appendable.append(")");
    }
}
