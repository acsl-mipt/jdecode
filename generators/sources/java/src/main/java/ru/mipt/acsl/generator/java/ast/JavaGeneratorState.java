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

    public void eol()
    {
        append("\n");
    }

    public void indent()
    {
        append(StringUtils.repeat("\t", indentationDepth));
    }

    public void startBlock()
    {
        eol();
        indent();
        append("{");
        eol();
        indentationDepth++;
    }

    public void finishBlock()
    {
        eol();
        indentationDepth--;
        indent();
        append("}");
    }

    public JavaGeneratorState append(@NotNull String str)
    {
        try
        {
            appendable.append(str);
            return this;
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }
}
