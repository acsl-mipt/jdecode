package ru.mipt.acsl.decode.model;

import ru.mipt.acsl.decode.model.expr.ConstExpr;
import ru.mipt.acsl.decode.model.naming.Container;
import ru.mipt.acsl.decode.model.proxy.MaybeProxy;
import ru.mipt.acsl.decode.model.registry.Measure;
import ru.mipt.acsl.decode.model.registry.Registry;
import ru.mipt.acsl.decode.model.types.*;

/**
 * @author Artem Shein
 */
public abstract class AbstractReferenceableVisitor implements ReferenceableVisitor {

    @Override
    public void visit(DecodeType t) {
    }

    @Override
    public void visit(Alias a) {
    }

    @Override
    public void visit(TmParameter p) {
    }

    @Override
    public void visit(Container c) {
    }

    @Override
    public void visit(EnumConstant c) {
    }

    @Override
    public void visit(Measure m) {
    }

    @Override
    public void visit(MaybeProxy p) {
    }

    @Override
    public void visit(Registry r) {
    }

    @Override
    public void visit(ConstExpr e) {
    }

    @Override
    public void visit(StructField f) {
    }

    @Override
    public void visit(EnumType e) {
    }

    @Override
    public void visit(StructType s) {
    }

}
