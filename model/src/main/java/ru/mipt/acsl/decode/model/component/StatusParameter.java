package ru.mipt.acsl.decode.model.component;

import ru.mipt.acsl.decode.model.TmParameter;
import ru.mipt.acsl.decode.model.component.message.MessageParameterRef;
import ru.mipt.acsl.decode.model.component.message.MessageParameterRefWalker;

/**
 * @author Artem Shein
 */
public interface StatusParameter extends TmParameter {

    MessageParameterPath path();

    default MessageParameterRef ref(Component component) {
        return new MessageParameterRefWalker(component, null, path());
    }

}
