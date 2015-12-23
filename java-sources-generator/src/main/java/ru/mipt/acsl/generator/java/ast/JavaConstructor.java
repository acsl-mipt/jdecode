package ru.mipt.acsl.generator.java.ast;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;

/**
 * @author Artem Shein
 */
public class JavaConstructor extends JavaClassMethod
{
    private static JavaType THIS_TYPE = new JavaTypeApplication("NO_TYPE");

    public JavaConstructor(@NotNull JavaVisibility visibility, @NotNull String name, @NotNull List<JavaMethodArgument> arguments,
                           @NotNull List<JavaStatement> statements)
    {
        super(visibility, false, THIS_TYPE, name, arguments, statements);
    }

    @Override
    public void generate(@NotNull JavaGeneratorState state)
    {
        visibility.generate(state);
        state.append(" ").append(name).append("(");
        if (!arguments.isEmpty())
        {
            boolean isFirst = true;
            for (JavaMethodArgument argument : arguments)
            {
                if (isFirst)
                {
                    isFirst = false;
                }
                else
                {
                    state.append(", ");
                }
                argument.generate(state);
            }
        }
        state.append(")");
        state.startBlock();
        if (!statements.isEmpty())
        {
            boolean isFirst = true;
            for (JavaStatement statement : statements)
            {
                if (isFirst)
                {
                    isFirst = false;
                }
                else
                {
                    state.eol();
                }
                state.indent();
                statement.generate(state);
                state.append(";");
            }
        }
        state.finishBlock();
    }
}
