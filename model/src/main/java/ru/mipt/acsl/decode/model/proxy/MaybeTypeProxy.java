package ru.mipt.acsl.decode.model.proxy;

import ru.mipt.acsl.decode.model.types.DecodeType;

/**
 * @author Artem Shein
 */
public interface MaybeTypeProxy extends MaybeProxy {

    DecodeType obj();

}
