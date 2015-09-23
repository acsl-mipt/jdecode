package ru.mipt.acsl.decode.parser;

import org.jetbrains.annotations.NotNull;
import org.parboiled.errors.ParseError;

/**
 * @author Artem Shein
 */
public class ParboiledErrorToStringWrapper
{
    @NotNull
    private final ParseError parseError;

    public ParboiledErrorToStringWrapper(@NotNull ParseError parseError)
    {
        this.parseError = parseError;
    }

    @NotNull
    @Override
    public String toString()
    {
        return String.format("%s at '%s'", parseError.getErrorMessage(), parseError.getInputBuffer().extract(parseError.getStartIndex(), parseError.getEndIndex()));
    }
}
