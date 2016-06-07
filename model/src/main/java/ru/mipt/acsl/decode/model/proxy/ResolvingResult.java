package ru.mipt.acsl.decode.model.proxy;

import org.jetbrains.annotations.Nullable;
import ru.mipt.acsl.decode.model.Message;

import java.util.Optional;

/**
 * @author Artem Shein
 */
public interface ResolvingResult<T> {

    static <T> ResolvingResult<T> newInstance() {
        return newInstance(null);
    }

    static <T> ResolvingResult<T> newInstance(Message msg) {
        return newInstance(null, ResolvingMessages.newInstance(msg));
    }

    static <T> ResolvingResult<T> newInstance(@Nullable T result) {
        return newInstance(result, ResolvingMessages.newInstance());
    }

    static <T> ResolvingResult<T> newInstance(@Nullable T result, Message message) {
        return newInstance(result, ResolvingMessages.newInstance(message));
    }

    static <T> ResolvingResult<T> newInstance(@Nullable T result, ResolvingMessages messages) {
        return new ResolvingResultImpl<>(result, messages);
    }

    Optional<T> result();

    ResolvingMessages messages();

    void messages(ResolvingMessages messages);

}
