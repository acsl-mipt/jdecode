package ru.mipt.acsl.decode.model.component;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Artem Shein
 */
public interface MessageParameterPath {

    static MessageParameterPath newInstance() {
        return newInstance(new ArrayList<>());
    }

    static MessageParameterPath newInstance(List<MessageParameterPathElement> elements) {
        return new MessageParameterPathImpl(elements);
    }

    List<MessageParameterPathElement> elements();

    default MessageParameterPathElement head() {
        return elements().get(0);
    }

    default MessageParameterPath tail() {
        return newInstance(elements().subList(1, elements().size()));
    }

}
