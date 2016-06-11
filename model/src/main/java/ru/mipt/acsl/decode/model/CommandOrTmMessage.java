package ru.mipt.acsl.decode.model;

import ru.mipt.acsl.decode.model.component.Command;
import ru.mipt.acsl.decode.model.component.message.TmMessage;
import ru.mipt.acsl.decode.model.naming.Container;

import java.util.Optional;

/**
 * Created by metadeus on 06.06.16.
 */
public interface CommandOrTmMessage extends Container {

    static CommandOrTmMessage newInstance(Command command) {
        return new CommandOrTmMessageImpl(command);
    }

    static CommandOrTmMessage newInstance(TmMessage tmMessage) {
        return new CommandOrTmMessageImpl(tmMessage);
    }

    boolean isCommand();

    boolean isTmMessage();

    Optional<Command> command();

    Optional<TmMessage> tmMessage();

    @Override
    default <T> T accept(ContainerVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
