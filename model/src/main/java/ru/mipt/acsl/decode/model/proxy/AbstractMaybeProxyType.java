package ru.mipt.acsl.decode.model.proxy;

import ru.mipt.acsl.decode.model.types.DecodeType;

/**
 * @author Artem Shein
 */
public abstract class AbstractMaybeProxyType<T extends DecodeType> extends AbstractMaybeProxy<T> implements MaybeTypeProxy {

    protected AbstractMaybeProxyType(Class<T> cls, Proxy proxy)
    {
        super(cls, proxy);
    }

    protected AbstractMaybeProxyType(Class<T> cls, T type)
    {
        super(cls, type);
    }

}
