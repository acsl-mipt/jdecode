package ru.mipt.acsl.decode.model.types;

import org.jetbrains.annotations.Nullable;
import ru.mipt.acsl.decode.model.naming.ElementName;
import ru.mipt.acsl.decode.model.naming.Namespace;

import java.util.List;

/**
 * Created by metadeus on 07.06.16.
 */
public interface Const extends DecodeType {

    static Const newInstance(@Nullable Alias.NsConst alias, Namespace namespace, String value,
                                                   List<ElementName> typeParameters) {
        return new ConstImpl(alias, namespace, value, typeParameters);
    }

    String value();

}
