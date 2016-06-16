package ru.mipt.acsl.decode.model.proxy;

import ru.mipt.acsl.decode.model.Message;
import ru.mipt.acsl.decode.model.Referenceable;
import ru.mipt.acsl.decode.model.types.DecodeType;

/**
 * @author Artem Shein
 */
public class MaybeTypeProxyType implements MaybeTypeProxy {

    private Proxy proxy;
    private DecodeType type;

    public static MaybeTypeProxyType newInstance(Proxy proxy) {
        return new MaybeTypeProxyType(proxy);
    }

    public static MaybeTypeProxyType newInstance(DecodeType type) {
        return new MaybeTypeProxyType(type);
    }

    @Override
    public boolean isResolved() {
        return type != null;
    }


    @Override
    public DecodeType obj() {
        if (type == null)
            throw new AssertionError("proxy is not resolved");
        return type;
    }

    @Override
    public <T> T accept(MaybeProxyVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public Proxy proxy() {
        if (proxy == null)
            throw new AssertionError("proxy resolved");
        return proxy;
    }

    @Override
    public ResolvingMessages resolveTo(Referenceable obj) {
        if (!(obj instanceof DecodeType))
            return ResolvingMessages.newInstance(Message.newInstance(Level.ERROR, String.format("%s is not a type", obj)));
        this.type = (DecodeType) obj;
        this.proxy = null;
        return ResolvingMessages.newInstance();
    }

    private MaybeTypeProxyType(Proxy proxy) {
        this.proxy = proxy;
        this.type = null;
    }

    private MaybeTypeProxyType(DecodeType type) {
        this.type = type;
        this.proxy = null;
    }

}
