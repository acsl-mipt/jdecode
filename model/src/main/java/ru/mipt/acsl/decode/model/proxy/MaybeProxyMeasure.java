package ru.mipt.acsl.decode.model.proxy;

import ru.mipt.acsl.decode.model.registry.Measure;

/**
 * @author Artem Shein
 */
public class MaybeProxyMeasure extends AbstractMaybeProxy<Measure>
{

    public static MaybeProxyMeasure newInstance(Proxy proxy)
    {
        return new MaybeProxyMeasure(proxy);
    }

    public static MaybeProxyMeasure newInstance(Measure measure)
    {
        return new MaybeProxyMeasure(measure);
    }

    @Override
    public <T> T accept(MaybeProxyVisitor<T> visitor)
    {
        return visitor.visit(this);
    }

    private MaybeProxyMeasure(Proxy proxy)
    {
        super(Measure.class, proxy);
    }

    private MaybeProxyMeasure(Measure measure)
    {
        super(Measure.class, measure);
    }

}
