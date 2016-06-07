package ru.mipt.acsl.decode.model.component;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.mipt.acsl.decode.model.component.message.ArrayRange;
import ru.mipt.acsl.decode.model.naming.ElementName;

import java.util.Optional;

/**
 * @author Artem Shein
 */
public class MessageParameterPathElementImpl implements MessageParameterPathElement {

    @Nullable
    private final ElementName elementName;
    @Nullable
    private final ArrayRange arrayRange;

    MessageParameterPathElementImpl(@NotNull ElementName elementName) {
        this.elementName = elementName;
        this.arrayRange = null;
    }

    MessageParameterPathElementImpl(@NotNull ArrayRange arrayRange) {
        this.arrayRange = arrayRange;
        this.elementName = null;
    }

    @Override
    public boolean isElementName() {
        return elementName != null;
    }

    @Override
    public boolean isArrayRange() {
        return arrayRange != null;
    }

    @Override
    public Optional<ElementName> elementName() {
        return Optional.ofNullable(elementName);
    }

    @Override
    public Optional<ArrayRange> arrayRange() {
        return Optional.ofNullable(arrayRange);
    }
}
