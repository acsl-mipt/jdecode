package ru.mipt.acsl.decode.model;

import java.util.List;

/**
 * @author Artem Shein
 */
class ValidatingResultImpl implements ValidatingResult {

    private final List<Message> messages;

    ValidatingResultImpl(List<Message> messages) {
        this.messages = messages;
    }

    @Override
    public boolean add(Message message) {
        return messages.add(message);
    }

    @Override
    public boolean addAll(ValidatingResult r) {
        return messages.addAll(r.messages());
    }

    @Override
    public List<Message> messages() {
        return messages;
    }
}
