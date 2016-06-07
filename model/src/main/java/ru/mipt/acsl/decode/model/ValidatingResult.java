package ru.mipt.acsl.decode.model;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Artem Shein
 */
public interface ValidatingResult {

    static ValidatingResult newInstance() {
        return new ValidatingResultImpl(new ArrayList<>());
    }

    boolean add(Message message);

    boolean addAll(ValidatingResult r);

    List<Message> messages();
}
