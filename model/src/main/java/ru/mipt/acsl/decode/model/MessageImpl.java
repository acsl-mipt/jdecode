package ru.mipt.acsl.decode.model;

import ru.mipt.acsl.decode.model.proxy.Level;

/**
 * @author Artem Shein
 */
public class MessageImpl implements Message {

    private final Level level;
    private final String text;

    MessageImpl(Level level, String text) {
        this.level = level;
        this.text = text;
    }

    @Override
    public String text()
    {
        return text;
    }

    @Override
    public Level level()
    {
        return level;
    }
}
