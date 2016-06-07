package ru.mipt.acsl.decode.model.types;

import ru.mipt.acsl.decode.model.naming.Container;
import ru.mipt.acsl.decode.model.naming.ElementName;
import ru.mipt.acsl.decode.model.registry.Language;

import java.util.Map;

/**
 * Created by metadeus on 06.06.16.
 */
class AbstractTypeAlias<P extends Container, O extends DecodeType> extends AbstractAlias<P, O> implements TypeAlias<P, O> {

    AbstractTypeAlias(ElementName name, Map<Language, String> info, P parent, O obj) {
        super(name, info, parent, obj);
    }

}
