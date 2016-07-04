package ru.mipt.acsl.decode.model.proxy;

import ru.mipt.acsl.decode.model.types.Const;
import ru.mipt.acsl.decode.model.types.DecodeType;

/**
 * @author Artem Shein
 */
public class MaybeProxyConst extends AbstractMaybeProxyType<Const> {

    public static MaybeProxyConst newInstance(Proxy proxy) {
        return new MaybeProxyConst(proxy);
    }

    public static MaybeProxyConst newInstance(Const _const) {
        return new MaybeProxyConst(_const);
    }

    @Override
    public <T> T accept(MaybeProxyVisitor<T> visitor) {
        return visitor.visit(this);
    }

    private MaybeProxyConst(Proxy proxy)
    {
        super(Const.class, proxy);
    }

    private MaybeProxyConst(Const _const)
    {
        super(Const.class, _const);
    }

}
