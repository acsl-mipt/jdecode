package ru.mipt.acsl.decode.model.domain.type;

import ru.mipt.acsl.decode.model.domain.DecodeNameAware;
import ru.mipt.acsl.decode.model.domain.impl.ImmutableDecodeBerType;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Artem Shein
 */
public interface DecodeNativeType extends DecodeType, DecodeNameAware
{
    Set<String> MANGLED_TYPE_NAMES = new HashSet<String>(){{ add(ImmutableDecodeBerType.MANGLED_NAME.asString()); }};
}
