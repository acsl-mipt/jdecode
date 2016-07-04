package ru.mipt.acsl.decode.model.proxy;

import ru.mipt.acsl.decode.model.types.DecodeType;

/**
 * @author Artem Shein
 */
public class MaybeProxyType extends AbstractMaybeProxyType<DecodeType> {

    public static MaybeProxyType newInstance(Proxy proxy) {
        return new MaybeProxyType(proxy);
    }

    public static MaybeProxyType newInstance(DecodeType type) {
        return new MaybeProxyType(type);
    }

    @Override
    public <T> T accept(MaybeProxyVisitor<T> visitor) {
        return visitor.visit(this);
    }

    private MaybeProxyType(Proxy proxy)
    {
        super(DecodeType.class, proxy);
    }

    private MaybeProxyType(DecodeType type)
    {
        super(DecodeType.class, type);
    }

}
