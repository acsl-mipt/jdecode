package ru.mipt.acsl.decode.model.proxy;

/**
 * Created by metadeus on 16.06.16.
 */
public interface MaybeProxyVisitor<T> {

    T visit(MaybeProxyNamespace n);

    T visit(MaybeProxyType tt);

    T visit(MaybeProxyConst c);

    T visit(MaybeProxyEnumType e);

    T visit(MaybeProxyStructType s);

    T visit(MaybeProxyComponent c);

    T visit(MaybeProxyMeasure m);

    T visit(MaybeProxyReferenceable r);

}
