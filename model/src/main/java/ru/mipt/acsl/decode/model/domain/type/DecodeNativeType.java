package ru.mipt.acsl.decode.model.domain.type;

import ru.mipt.acsl.decode.model.domain.DecodeNameAware;
import ru.mipt.acsl.decode.model.domain.impl.type.ImmutableDecodeBerType;
import ru.mipt.acsl.decode.model.domain.impl.type.ImmutableDecodeOrType;
import ru.mipt.acsl.decode.model.domain.impl.type.DecodeOptionalType;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Artem Shein
 */
public interface DecodeNativeType extends DecodeType, DecodeNameAware
{
    Set<String> MANGLED_TYPE_NAMES = new HashSet<String>(){{
        add(ImmutableDecodeBerType.MANGLED_NAME.asString());
        add(ImmutableDecodeOrType.MANGLED_NAME.asString());
        add(DecodeOptionalType.MANGLED_NAME().asString());
    }};
}
