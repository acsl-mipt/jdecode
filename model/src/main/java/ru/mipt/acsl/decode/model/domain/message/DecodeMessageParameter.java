package ru.mipt.acsl.decode.model.domain.message;

import org.jetbrains.annotations.NotNull;
import ru.mipt.acsl.decode.model.domain.DecodeElement;

/**
 * @author Artem Shein
 */
public interface DecodeMessageParameter extends DecodeElement
{
    @NotNull
    String getValue();

}
