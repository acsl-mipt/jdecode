package ru.mipt.acsl.decode.model.proxy;

import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * @author Artem Shein
 */
public class ResolvingResultImpl<T> implements ResolvingResult<T> {

    private final T result;
    private ResolvingMessages messages;

    ResolvingResultImpl(@Nullable T result, ResolvingMessages messages) {
        this.result = result;
        this.messages = messages;
    }

    @Override
    public Optional<T> result() {
        return Optional.ofNullable(result);
    }

    @Override
    public ResolvingMessages messages() {
        return messages;
    }

    @Override
    public void messages(ResolvingMessages messages) {
        this.messages = messages;
    }
}
