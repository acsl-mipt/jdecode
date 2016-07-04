package ru.mipt.acsl.decode.model.proxy;

import com.google.common.base.Preconditions;
import ru.mipt.acsl.decode.model.Message;
import ru.mipt.acsl.decode.model.Referenceable;

/**
 * @author Artem Shein
 */
public abstract class AbstractMaybeProxy<T extends Referenceable> implements MaybeProxy {

    private Class<T> cls;
    private Proxy proxy;
    private T obj;

    @Override
    public boolean isResolved()
    {
        return obj != null;
    }

    @Override
    public T obj()
    {
        return Preconditions.checkNotNull(obj);
    }

    @Override
    public Proxy proxy()
    {
        return Preconditions.checkNotNull(proxy);
    }

    @Override
    public ResolvingMessages resolveTo(Referenceable obj)
    {
        if (!cls.isInstance(obj))
            return ResolvingMessages.newInstance(Message.newError(String.format("%s is not a %s", obj, cls.getSimpleName())));
        this.obj = (T) obj;
        return ResolvingMessages.newInstance();
    }

    protected AbstractMaybeProxy(Class<T> cls, Proxy proxy)
    {
        this.cls = cls;
        this.proxy = proxy;
    }

    protected AbstractMaybeProxy(Class<T> cls, T obj)
    {
        this.cls = cls;
        this.obj = obj;
    }

}

