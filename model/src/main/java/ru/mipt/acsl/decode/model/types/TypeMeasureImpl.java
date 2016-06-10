package ru.mipt.acsl.decode.model.types;

import org.jetbrains.annotations.Nullable;
import ru.mipt.acsl.decode.model.naming.Namespace;
import ru.mipt.acsl.decode.model.proxy.MaybeProxyCompanion;
import ru.mipt.acsl.decode.model.proxy.MaybeTypeProxy;

import java.util.Optional;

/**
 * Created by metadeus on 07.06.16.
 */
public class TypeMeasureImpl implements TypeMeasure {

    private final MaybeTypeProxy typeProxy;
    @Nullable
    private final MaybeProxyCompanion.Measure measureProxy;

    TypeMeasureImpl(MaybeTypeProxy typeProxy, @Nullable MaybeProxyCompanion.Measure measureProxy) {
        this.typeProxy = typeProxy;
        this.measureProxy = measureProxy;
    }

    @Override
    public void namespace(Namespace ns) {
        typeProxy.obj().namespace(ns);
    }


    @Override
    public String toString() {
        return String.format("%s{typeProxy = %s, measureProxy = %s}", getClass(), typeProxy, measureProxy);
    }

    @Override
    public MaybeTypeProxy typeProxy() {
        return typeProxy;
    }

    @Override
    public Optional<MaybeProxyCompanion.Measure> measureProxy() {
        return Optional.ofNullable(measureProxy);
    }
}
