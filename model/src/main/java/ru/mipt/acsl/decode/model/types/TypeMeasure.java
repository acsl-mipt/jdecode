package ru.mipt.acsl.decode.model.types;

import org.jetbrains.annotations.Nullable;
import ru.mipt.acsl.decode.model.naming.ElementName;
import ru.mipt.acsl.decode.model.naming.Namespace;
import ru.mipt.acsl.decode.model.proxy.MaybeProxyCompanion;
import ru.mipt.acsl.decode.model.proxy.MaybeTypeProxy;
import ru.mipt.acsl.decode.model.registry.Measure;

import java.util.List;
import java.util.Optional;

/**
 * Created by metadeus on 07.06.16.
 */
public interface TypeMeasure extends DecodeType {

    static TypeMeasure newInstance(MaybeTypeProxy typeProxy, @Nullable MaybeProxyCompanion.Measure measureProxy) {
        return new TypeMeasureImpl(typeProxy, measureProxy);
    }

    MaybeTypeProxy typeProxy();

    default DecodeType t() {
        return typeProxy().obj();
    }

    Optional<MaybeProxyCompanion.Measure> measureProxy();

    default Optional<Measure> measure() {
        return measureProxy().map(MaybeProxyCompanion.Measure::obj);
    }

    @Override
    default Namespace namespace() {
        return t().namespace();
    }

    @Override
    default Optional<Alias> alias() {
        return t().alias();
    }

    @Override
    default String systemName() {
        return t().systemName();
    }

    @Override
    default List<ElementName> typeParameters() {
        return t().typeParameters();
    }
    
}
