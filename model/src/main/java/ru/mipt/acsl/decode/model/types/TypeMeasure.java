package ru.mipt.acsl.decode.model.types;

import org.jetbrains.annotations.Nullable;
import ru.mipt.acsl.decode.model.Referenceable;
import ru.mipt.acsl.decode.model.ReferenceableVisitor;
import ru.mipt.acsl.decode.model.naming.ElementName;
import ru.mipt.acsl.decode.model.naming.Namespace;
import ru.mipt.acsl.decode.model.proxy.MaybeProxyMeasure;
import ru.mipt.acsl.decode.model.proxy.MaybeTypeProxy;
import ru.mipt.acsl.decode.model.registry.Measure;

import java.util.List;
import java.util.Optional;

/**
 * Created by metadeus on 07.06.16.
 */
public interface TypeMeasure extends Referenceable {

    static TypeMeasure newInstance(MaybeTypeProxy typeProxy, @Nullable MaybeProxyMeasure measureProxy) {
        return new TypeMeasureImpl(typeProxy, measureProxy);
    }

    MaybeTypeProxy typeProxy();

    default DecodeType t() {
        return typeProxy().obj();
    }

    Optional<MaybeProxyMeasure> measureProxy();

    default Optional<Measure> measure() {
        return measureProxy().map(MaybeProxyMeasure::obj);
    }

    default Namespace namespace() {
        return t().namespace();
    }

    default Optional<Alias> alias() {
        return t().alias();
    }

    default String systemName() {
        return t().systemName();
    }

    default List<ElementName> typeParameters() {
        return t().typeParameters();
    }

    @Override
    default <T> T accept(ReferenceableVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
