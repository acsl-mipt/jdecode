package ru.mipt.acsl.decode.model.proxy;

import ru.mipt.acsl.decode.model.Referenceable;
import ru.mipt.acsl.decode.model.registry.Measure;

/**
 * @author Artem Shein
 */
public class MaybeProxyReferenceable extends AbstractMaybeProxy<Referenceable>
{

    public static MaybeProxyReferenceable newInstance(Proxy proxy)
    {
        return new MaybeProxyReferenceable(proxy);
    }

    public static MaybeProxyReferenceable newInstance(Referenceable referenceable)
    {
        return new MaybeProxyReferenceable(referenceable);
    }

    @Override
    public <T> T accept(MaybeProxyVisitor<T> visitor)
    {
        return visitor.visit(this);
    }

    private MaybeProxyReferenceable(Proxy proxy)
    {
        super(Referenceable.class, proxy);
    }

    private MaybeProxyReferenceable(Referenceable referenceable)
    {
        super(Referenceable.class, referenceable);
    }

}
