package ru.mipt.acsl.generator.java.ast;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @author Artem Shein
 */
public class JavaEnum extends AbstractJavaBaseClass
{
    public static Builder newBuilder(@NotNull String packageFqn, @NotNull String enumName)
    {
        return new Builder(packageFqn, enumName);
    }

    @Override
    public void generate(@NotNull JavaGeneratorState state)
    {
        generateVisibility(state);
        state.append("enum ").append(name);
        state.startBlock();

        state.finishBlock();
    }

    public static class Builder extends AbstractJavaBaseClass.Builder
    {
        @NotNull
        private List<String> genericArguments = new ArrayList<>();

        public Builder(@NotNull String packageFqn, @NotNull String enumName)
        {
            super(packageFqn, enumName);
        }

        @NotNull
        @Override
        public JavaEnum build()
        {
            return new JavaEnum(visibility, isStatic, packageFqn, name, genericArguments, fields, methods, innerClasses);
        }
    }

    private JavaEnum(@NotNull JavaVisibility visibility, boolean isStatic, @NotNull String packageFqn, @NotNull String name,
                     @NotNull List<String> genericArguments, @NotNull List<JavaField> fields,
                     @NotNull List<JavaClassMethod> methods, @NotNull List<AbstractJavaBaseClass> innerClasses)
    {
        super(visibility, isStatic, packageFqn, name, genericArguments, Optional.empty(), fields, methods, innerClasses, new ArrayList<>());
    }
}
