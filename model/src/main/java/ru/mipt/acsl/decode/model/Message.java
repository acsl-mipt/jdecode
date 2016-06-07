package ru.mipt.acsl.decode.model;

import ru.mipt.acsl.decode.model.proxy.Level;

/**
 * @author Artem Shein
 */
public interface Message {

    static Message newError(String text) {
        return new MessageImpl(Level.ERROR, text);
    }

    static Message newInstance(Level level, String text) {
        return new MessageImpl(level, text);
    }

    Level level();

    String text();

}
