package ru.mipt.acsl.decode.model.proxy;

import com.google.common.collect.Lists;
import ru.mipt.acsl.decode.model.Message;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Artem Shein
 */
public interface ResolvingMessages {

    static ResolvingMessages newInstance() {
        return newInstance(new ArrayList<>());
    }

    static ResolvingMessages newInstance(Message message) {
        return newInstance(Lists.newArrayList(message));
    }

    static ResolvingMessages newInstance(List<Message> messages) {
        return new ResolvingMessagesImpl(messages);
    }

    List<Message> messages();

    default boolean hasError() {
        return messages().stream().anyMatch(m -> m.level() == Level.ERROR);
    }

    default boolean addAll(ResolvingMessages messages) {
        return messages().addAll(messages.messages());
    }
}
