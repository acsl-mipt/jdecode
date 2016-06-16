package ru.mipt.acsl.decode.model;

import ru.mipt.acsl.decode.model.component.Command;
import ru.mipt.acsl.decode.model.component.Component;
import ru.mipt.acsl.decode.model.component.message.TmMessage;
import ru.mipt.acsl.decode.model.naming.Namespace;
import ru.mipt.acsl.decode.model.types.EnumType;
import ru.mipt.acsl.decode.model.types.StructType;

/**
 * Created by metadeus on 11.06.16.
 */
public interface ContainerVisitor<T> {

    T visit(Namespace namespace);

    T visit(Command command);

    default T visit(MessageOrCommand messageOrCommand) {
        return messageOrCommand.isCommand()
                ? visit(messageOrCommand.command().get())
                : visit(messageOrCommand.message().get());
    }

    T visit(EnumType enumType);

    T visit(StructType structType);

    T visit(TmMessage tmMessage);

    T visit(Component component);

}
