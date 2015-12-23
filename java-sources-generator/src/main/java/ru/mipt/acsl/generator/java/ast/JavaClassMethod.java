package ru.mipt.acsl.generator.java.ast;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;

/**
 * @author Artem Shein
 */
public class JavaClassMethod implements JavaAstElement
{
    @NotNull
    protected final JavaVisibility visibility;
    protected final boolean isStatic;
    @NotNull
    protected JavaType returnType;
    @NotNull
    protected final String name;
    @NotNull
    protected final List<JavaMethodArgument> arguments;
    @NotNull
    protected final List<JavaStatement> statements;

    @NotNull
    public JavaType getReturnType()
    {
        return returnType;
    }

    @NotNull
    public List<JavaMethodArgument> getArguments()
    {
        return arguments;
    }

    public JavaClassMethod(@NotNull JavaVisibility visibility, boolean isStatic, @NotNull JavaType returnType,
                           @NotNull String name, @NotNull List<JavaMethodArgument> arguments,
                           @NotNull List<JavaStatement> statements)
    {
        this.visibility = visibility;
        this.isStatic = isStatic;
        this.returnType = returnType;
        this.name = name;
        this.arguments = arguments;
        this.statements = statements;
    }

    @Override
    public void generate(@NotNull JavaGeneratorState state)
    {
        visibility.generate(state);
        if (isStatic)
        {
            state.append(" static");
        }
        state.append(" ");
        returnType.generate(state);
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

    public void setReturnType(@NotNull JavaType returnType)
    {
        this.returnType = returnType;
    }
}
