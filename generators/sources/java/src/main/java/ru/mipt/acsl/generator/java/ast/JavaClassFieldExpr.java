package ru.mipt.acsl.generator.java.ast;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * @author Artem Shein
 */
public class JavaClassFieldExpr implements JavaExpr
{
    @NotNull
    private final JavaTypeApplication typeApplication;
    @NotNull
    private final String fieldName;

    public JavaClassFieldExpr(@NotNull JavaTypeApplication typeApplication, @NotNull String fieldName)
    {
        this.typeApplication = typeApplication;
        this.fieldName = fieldName;
    }

    @Override
    public void generate(@NotNull JavaGeneratorState state)
    {
        typeApplication.generate(state);
        state.append(".").append(fieldName);
    }
}
