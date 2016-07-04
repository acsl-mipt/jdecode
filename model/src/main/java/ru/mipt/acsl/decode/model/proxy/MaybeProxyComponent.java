package ru.mipt.acsl.decode.model.proxy;

import ru.mipt.acsl.decode.model.component.Component;
import ru.mipt.acsl.decode.model.registry.Measure;

/**
 * @author Artem Shein
 */
public class MaybeProxyComponent extends AbstractMaybeProxy<Component>
{

    public static MaybeProxyComponent newInstance(Proxy proxy)
    {
        return new MaybeProxyComponent(proxy);
    }

    public static MaybeProxyComponent newInstance(Component component)
    {
        return new MaybeProxyComponent(component);
    }

    @Override
    public <T> T accept(MaybeProxyVisitor<T> visitor)
    {
        return visitor.visit(this);
    }

    private MaybeProxyComponent(Proxy proxy)
    {
        super(Component.class, proxy);
    }

    private MaybeProxyComponent(Component component)
    {
        super(Component.class, component);
    }

}
