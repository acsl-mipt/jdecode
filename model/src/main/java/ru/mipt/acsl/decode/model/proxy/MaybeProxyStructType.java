package ru.mipt.acsl.decode.model.proxy;

import ru.mipt.acsl.decode.model.types.StructType;

/**
 * @author Artem Shein
 */
public class MaybeProxyStructType extends AbstractMaybeProxyType<StructType> {

    public static MaybeProxyStructType newInstance(Proxy proxy)
    {
        return new MaybeProxyStructType(proxy);
    }

    public static MaybeProxyStructType newInstance(StructType structType)
    {
        return new MaybeProxyStructType(structType);
    }

    @Override
    public <T> T accept(MaybeProxyVisitor<T> visitor)
    {
        return visitor.visit(this);
    }

    private MaybeProxyStructType(Proxy proxy)
    {
        super(StructType.class, proxy);
    }

    private MaybeProxyStructType(StructType structType)
    {
        super(StructType.class, structType);
    }

}
