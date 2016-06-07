package ru.mipt.acsl.decode.model.proxy;

import ru.mipt.acsl.decode.model.Message;

import java.util.List;

/**
 * @author Artem Shein
 */
public class ResolvingMessagesImpl implements ResolvingMessages {

    private final List<Message> messages;

    ResolvingMessagesImpl(List<Message> messages) {
        this.messages = messages;
    }

    @Override
    public List<Message> messages() {
        return messages;
    }
}
