package ru.mipt.acsl.decode.model.registry;

import ru.mipt.acsl.decode.model.Parameter;
import ru.mipt.acsl.decode.model.Referenceable;
import ru.mipt.acsl.decode.model.ReferenceableVisitor;
import ru.mipt.acsl.decode.model.TmParameter;
import ru.mipt.acsl.decode.model.component.Command;
import ru.mipt.acsl.decode.model.component.message.EventMessage;
import ru.mipt.acsl.decode.model.expr.ConstExpr;
import ru.mipt.acsl.decode.model.naming.Container;
import ru.mipt.acsl.decode.model.proxy.MaybeProxy;
import ru.mipt.acsl.decode.model.proxy.MaybeProxyCompanion;
import ru.mipt.acsl.decode.model.proxy.ResolvingMessages;
import ru.mipt.acsl.decode.model.types.*;

import java.util.List;
import java.util.Optional;

/**
 * @author Artem Shein
 */
public class RegistryUtils {

    public static ResolvingMessages resolve(Registry r) {
        return RegistryUtils.resolve(r, r.rootNamespace());
    }

    public static ResolvingMessages resolve(Registry r, Referenceable obj) {
        final ResolvingMessages result = ResolvingMessages.newInstance();
        obj.accept(new ReferenceableVisitor<Void>(){

            @Override
            public Void visit(EnumType e) {
                visit((Container) e);
                MaybeProxyEnumOrTypeMeasure proxy = e.extendsOrBaseTypeProxy();
                Optional<MaybeProxyCompanion.Enum> en = proxy.maybeProxyEnum();
                if (en.isPresent())
                    result.addAll(en.get().resolve(r));
                else
                    result.addAll(proxy.typeMeasure().get().typeProxy().resolve(r)); // must be not null
                return null;
            }

            @Override
            public Void visit(Measure m) {
                return null;
            }

            @Override
            public Void visit(MaybeProxy p) {
                result.addAll(p.resolve(r));
                return null;
            }

            @Override
            public Void visit(Registry r) {
                visit(r.rootNamespace());
                return null;
            }

            @Override
            public Void visit(ConstExpr e) {
                return null;
            }

            @Override
            public Void visit(Container c) {
                List<Referenceable> objects = c.objects();
                // do not replace with stream, wee add elements inside, ConcurrentModificationException may occur
                for (int index = 0; index < objects.size(); index++) {
                    result.addAll(resolve(r, objects.get(index)));
                }
                if (c instanceof Command)
                    result.addAll(((Command) c).returnTypeProxy().resolve(r));
                else if (c instanceof EventMessage)
                    result.addAll(((EventMessage) c).baseTypeProxy().resolve(r));
                return null;
            }

            @Override
            public Void visit(EnumConstant c) {
                return null;
            }

            @Override
            public Void visit(Alias a) {
                if (a instanceof Alias.ComponentComponent)
                    result.addAll(((Alias.ComponentComponent) a).obj().resolve(r));
                return null;
            }

            @Override
            public Void visit(DecodeType t) {
                if (t instanceof SubType && !t.isGeneric())
                    result.addAll(((SubType) t).typeMeasure().typeProxy().resolve(r));
                else if (t instanceof TypeMeasure) {
                    TypeMeasure tm = (TypeMeasure) t;
                    result.addAll(tm.typeProxy().resolve(r));
                    tm.measureProxy().ifPresent(p -> result.addAll(p.resolve(r)));
                }
                return null;
            }

            @Override
            public Void visit(TmParameter tmParameter) {
                if (tmParameter instanceof Parameter) {
                    Parameter parameter = (Parameter) tmParameter;
                    result.addAll(parameter.typeProxy().resolve(r));
                    parameter.measureProxy().ifPresent(p -> result.addAll(p.resolve(r)));
                }
                return null;
            }

            @Override
            public Void visit(StructField f) {
                result.addAll(f.typeMeasure().typeProxy().resolve(r));
                f.typeMeasure().measureProxy().ifPresent(p -> result.addAll(p.resolve(r)));
                return null;
            }

            @Override
            public Void visit(StructType s) {
                visit((Container) s);
                return null;
            }

        });
        return result;
    }

    // prevent from instantiating
    private RegistryUtils() {

    }
}
