package ru.mipt.acsl.generator.java.ast;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * @author Artem Shein
 */
public enum JavaVisibility implements JavaAstElement
{
    PUBLIC("public"), PROTECTED("protected"), PACKAGE_PRIVATE(""), PRIVATE("private");

    @NotNull
    private final String name;

    JavaVisibility(@NotNull String name)
    {
        this.name = name;
    }

    @Override
    public void generate(@NotNull JavaGeneratorState state)
    {
        state.append(name);
    }
}
