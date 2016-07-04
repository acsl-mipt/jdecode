package ru.mipt.acsl.decode.model.proxy;

import ru.mipt.acsl.decode.model.types.EnumType;

/**
 * @author Artem Shein
 */
public class MaybeProxyEnumType extends AbstractMaybeProxyType<EnumType> {

    public static MaybeProxyEnumType newInstance(Proxy proxy)
    {
        return new MaybeProxyEnumType(proxy);
    }

    public static MaybeProxyEnumType newInstance(EnumType enumType)
    {
        return new MaybeProxyEnumType(enumType);
    }

    @Override
    public <T> T accept(MaybeProxyVisitor<T> visitor)
    {
        return visitor.visit(this);
    }

    private MaybeProxyEnumType(Proxy proxy)
    {
        super(EnumType.class, proxy);
    }

    private MaybeProxyEnumType(EnumType enumType)
    {
        super(EnumType.class, enumType);
    }

}
