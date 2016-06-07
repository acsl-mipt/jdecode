package ru.mipt.acsl.decode.model.component;

import java.util.List;

/**
 * @author Artem Shein
 */
public class MessageParameterPathImpl implements MessageParameterPath {

    private List<MessageParameterPathElement> elements;

    MessageParameterPathImpl(List<MessageParameterPathElement> elements) {
        this.elements = elements;
    }

    @Override
    public List<MessageParameterPathElement> elements() {
        return elements;
    }
}
