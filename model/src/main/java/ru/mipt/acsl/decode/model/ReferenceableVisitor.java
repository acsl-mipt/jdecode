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
public interface ReferenceableVisitor<T>
{
    T visit(DecodeType t);
    T visit(Alias a);
    T visit(TmParameter p);
    T visit(Container c);
    T visit(EnumConstant c);
    T visit(EnumType e);
    T visit(Measure m);
    T visit(MaybeProxy p);
    T visit(Registry r);
    T visit(ConstExpr e);
    T visit(StructField f);
    T visit(StructType s);
    T visit(TypeMeasure tm);
}
