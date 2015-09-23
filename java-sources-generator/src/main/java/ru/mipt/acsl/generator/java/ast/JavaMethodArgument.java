package ru.mipt.acsl.generator.java.ast;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * @author Artem Shein
 */
public class JavaMethodArgument implements JavaAstElement
{
    @NotNull
    private JavaType type;
    @NotNull
    private final String name;

    public JavaMethodArgument(@NotNull JavaType type, @NotNull String name)
    {
        this.type = type;
        this.name = name;
    }

    @NotNull
    public JavaType getType()
    {
        return type;
    }

    @Override
    public void generate(@NotNull JavaGeneratorState state, @NotNull Appendable appendable) throws IOException
    {
        type.generate(state, appendable);
        appendable.append(" ").append(name);
    }

    public void setType(@NotNull JavaType type)
    {
        this.type = type;
    }
}
