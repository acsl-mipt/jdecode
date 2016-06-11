package ru.mipt.acsl.decode.model;

import org.jetbrains.annotations.Nullable;
import ru.mipt.acsl.decode.model.expr.ConstExpr;
import ru.mipt.acsl.decode.model.naming.Container;
import ru.mipt.acsl.decode.model.proxy.MaybeProxy;
import ru.mipt.acsl.decode.model.proxy.MaybeProxyCompanion;
import ru.mipt.acsl.decode.model.registry.Measure;
import ru.mipt.acsl.decode.model.registry.Registry;
import ru.mipt.acsl.decode.model.types.*;

/**
 * @author Artem Shein
 */
public abstract class AbstractReferenceableVisitor<T> implements ReferenceableVisitor<T> {

    @Nullable
    @Override
    public T visit(DecodeType t) {
        return null;
    }

    @Nullable
    @Override
    public T visit(Alias a) {
        return null;
    }

    @Nullable
    @Override
    public T visit(TmParameter p) {
        return null;
    }

    @Nullable
    @Override
    public T visit(Container c) {
        return null;
    }

    @Nullable
    @Override
    public T visit(EnumConstant c) {
        return null;
    }

    @Nullable
    @Override
    public T visit(Measure m) {
        return null;
    }

    @Nullable
    @Override
    public T visit(MaybeProxy p) {
        return null;
    }

    @Nullable
    @Override
    public T visit(Registry r) {
        return null;
    }

    @Nullable
    @Override
    public T visit(ConstExpr e) {
        return null;
    }

    @Nullable
    @Override
    public T visit(StructField f) {
        return null;
    }

    @Nullable
    @Override
    public T visit(EnumType e) {
        return null;
    }

    @Nullable
    @Override
    public T visit(StructType s) {
        return null;
    }

}
