package ru.mipt.acsl.decode.model.component;

import ru.mipt.acsl.decode.model.component.message.ArrayRange;
import ru.mipt.acsl.decode.model.naming.ElementName;

import java.util.Optional;

/**
 * @author Artem Shein
 */
public interface MessageParameterPathElement {

    static MessageParameterPathElement newInstance(ElementName elmentName) {
        return new MessageParameterPathElementImpl(elmentName);
    }

    static MessageParameterPathElement newInstance(ArrayRange arrayRange) {
        return new MessageParameterPathElementImpl(arrayRange);
    }

    boolean isElementName();

    boolean isArrayRange();

    Optional<ElementName> elementName();

    Optional<ArrayRange> arrayRange();

}
