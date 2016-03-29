package ru.mipt.acsl.generator.java.ast;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Optional;

/**
 * @author Artem Shein
 */
public class JavaField implements JavaAstElement
{
    @NotNull
    private final JavaVisibility visibility;
    private final boolean isStatic;
    private final boolean isFinal;
    @NotNull
    private JavaType type;
    @NotNull
    private final String name;
    @NotNull
    private final Optional<JavaExpr> value;

    public JavaField(@NotNull JavaVisibility visibility, boolean isStatic, boolean isFinal,
                     @NotNull JavaType type,
                     @NotNull String name,
                     @NotNull Optional<JavaExpr> value)
    {
        this.visibility = visibility;
        this.isStatic = isStatic;
        this.isFinal = isFinal;
        this.type = type;
        this.name = name;
        this.value = value;
    }

    public JavaField(@NotNull JavaVisibility visibility, boolean isStatic, boolean isFinal, @NotNull JavaType type,
                     @NotNull String name)
    {
        this(visibility, isStatic, isFinal, type, name, Optional.empty());
    }

    @NotNull
    public JavaType getType()
    {
        return type;
    }

    @Override
    public void generate(@NotNull JavaGeneratorState state)
    {
        visibility.generate(state);
        state.append(" ");
        if (isStatic)
        {
            state.append("static ");
        }
        if (isFinal)
        {
            state.append("final ");
        }
        type.generate(state);
        state.append(" ").append(name);
        if (value.isPresent())
        {
            state.append(" = ");
            value.get().generate(state);
        }
        state.append(";");
    }

    public void setType(@NotNull JavaType type)
    {
        this.type = type;
    }
}
