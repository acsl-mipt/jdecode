package ru.mipt.acsl.decode.model.component.message;

import ru.mipt.acsl.decode.model.component.Component;
import ru.mipt.acsl.decode.model.component.MessageParameterPath;
import ru.mipt.acsl.decode.model.types.DecodeType;
import ru.mipt.acsl.decode.model.types.StructField;

import java.util.Optional;

/**
 * @author Artem Shein
 */
public interface MessageParameterRef {

    Component component();

    Optional<StructField> structField();

    MessageParameterPath path();

    DecodeType resultType();

    DecodeType t();

}
