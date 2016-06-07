package ru.mipt.acsl.decode.model.types;

import org.jetbrains.annotations.Nullable;
import ru.mipt.acsl.decode.model.naming.Namespace;
import ru.mipt.acsl.decode.model.proxy.MaybeProxy;

import java.util.Optional;

/**
 * Created by metadeus on 07.06.16.
 */
public class TypeMeasureImpl implements TypeMeasure {

    private final MaybeProxy.TypeProxy typeProxy;
    @Nullable
    private final MaybeProxy.Measure measureProxy;

    TypeMeasureImpl(MaybeProxy.TypeProxy typeProxy, @Nullable MaybeProxy.Measure measureProxy) {
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
    public MaybeProxy.TypeProxy typeProxy() {
        return typeProxy;
    }

    @Override
    public Optional<MaybeProxy.Measure> measureProxy() {
        return Optional.ofNullable(measureProxy);
    }
}
