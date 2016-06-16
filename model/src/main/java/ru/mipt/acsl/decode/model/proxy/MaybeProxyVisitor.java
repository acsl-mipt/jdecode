package ru.mipt.acsl.decode.model.proxy;

/**
 * Created by metadeus on 16.06.16.
 */
public interface MaybeProxyVisitor<T> {

    T visit(MaybeTypeProxyType tt);

    T visit(MaybeProxyCompanion.Enum e);

    T visit(MaybeProxyCompanion.Struct s);

    T visit(MaybeProxyCompanion.Component c);

    T visit(MaybeProxyCompanion.Measure m);

    T visit(MaybeProxyCompanion.Referenceable r);

}
