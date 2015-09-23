package ru.mipt.acsl.generator.java.ast;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * @author Artem Shein
 */
public class JavaGeneratorState
{
    @NotNull
    private final Appendable appendable;
    private int indentationDepth = 0;

    public JavaGeneratorState(@NotNull Appendable appendable)
    {
        this.appendable = appendable;
    }

    public void eol() throws IOException
    {
        appendable.append("\n");
    }

    public void indent() throws IOException
    {
        appendable.append(StringUtils.repeat("\t", indentationDepth));
    }

    public void startBlock() throws IOException
    {
        eol();
        indent();
        appendable.append("{");
        eol();
        indentationDepth++;
    }

    public void finishBlock() throws IOException
    {
        eol();
        indentationDepth--;
        indent();
        appendable.append("}");
    }
}
