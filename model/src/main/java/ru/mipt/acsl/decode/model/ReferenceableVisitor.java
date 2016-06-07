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
public interface ReferenceableVisitor
{
    void visit(DecodeType t);
    void visit(Alias a);
    void visit(TmParameter p);
    void visit(Container c);
    void visit(EnumConstant c);
    void visit(EnumType e);
    void visit(Measure m);
    void visit(MaybeProxy p);
    void visit(Registry r);
    void visit(ConstExpr e);
    void visit(StructField f);
    void visit(StructType s);
}
