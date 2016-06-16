package ru.mipt.acsl.decode.model;

import ru.mipt.acsl.decode.model.component.Command;
import ru.mipt.acsl.decode.model.component.message.TmMessage;
import ru.mipt.acsl.decode.model.naming.Container;

import java.util.Optional;

/**
 * Created by metadeus on 06.06.16.
 */
public interface MessageOrCommand extends Container {

    static MessageOrCommand newInstance(Command command) {
        return new MessageOrCommandImpl(command);
    }

    static MessageOrCommand newInstance(TmMessage message) {
        return new MessageOrCommandImpl(message);
    }

    boolean isCommand();

    boolean isMessage();

    Optional<Command> command();

    Optional<TmMessage> message();

    @Override
    default <T> T accept(ContainerVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
