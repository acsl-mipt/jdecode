package ru.mipt.acsl.decode.model;

import ru.mipt.acsl.decode.model.component.Command;
import ru.mipt.acsl.decode.model.component.message.TmMessage;

import java.util.List;
import java.util.Optional;

/**
 * Created by metadeus on 06.06.16.
 */
public class MessageOrCommandImpl implements MessageOrCommand {

    private final Command command;

    private final TmMessage tmMessage;

    MessageOrCommandImpl(Command command) {
        this.command = command;
        this.tmMessage = null;
    }

    MessageOrCommandImpl(TmMessage tmMessage) {
        this.tmMessage = tmMessage;
        this.command = null;
    }

    @Override
    public List<Referenceable> objects() {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void setObjects(List<Referenceable> objects) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public boolean isCommand() {
        return command != null;
    }

    @Override
    public boolean isMessage() {
        return tmMessage != null;
    }

    @Override
    public Optional<Command> command() {
        return Optional.ofNullable(command);
    }

    @Override
    public Optional<TmMessage> message() {
        return Optional.ofNullable(tmMessage);
    }
}
