package ru.mipt.acsl.decode.model.proxy;

import ru.mipt.acsl.decode.model.naming.Namespace;

/**
 * @author Artem Shein
 */
public class MaybeProxyNamespace extends AbstractMaybeProxy<Namespace>
{

    public static MaybeProxyNamespace newInstance(Proxy proxy)
    {
        return new MaybeProxyNamespace(proxy);
    }

    public static MaybeProxyNamespace newInstance(Namespace namespace)
    {
        return new MaybeProxyNamespace(namespace);
    }

    @Override
    public <T> T accept(MaybeProxyVisitor<T> visitor)
    {
        return visitor.visit(this);
    }

    private MaybeProxyNamespace(Proxy proxy)
    {
        super(Namespace.class, proxy);
    }

    private MaybeProxyNamespace(Namespace namespace)
    {
        super(Namespace.class, namespace);
    }

}
