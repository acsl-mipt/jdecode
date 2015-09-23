package ru.mipt.acsl.decode.modeling;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author Artem Shein
 */
public interface ResolvingResult
{
    boolean hasError();

    @NotNull
    List<ResolvingMessage> getMessages();
}
