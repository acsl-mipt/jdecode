package ru.mipt.acsl.decode.model.types;

import org.jetbrains.annotations.Nullable;
import ru.mipt.acsl.decode.model.naming.Namespace;
import ru.mipt.acsl.decode.model.proxy.MaybeProxyMeasure;
import ru.mipt.acsl.decode.model.proxy.MaybeTypeProxy;

import java.util.Optional;

/**
 * Created by metadeus on 07.06.16.
 */
public class TypeMeasureImpl implements TypeMeasure {

    private final MaybeTypeProxy typeProxy;
    @Nullable
    private final MaybeProxyMeasure measureProxy;

    TypeMeasureImpl(MaybeTypeProxy typeProxy, @Nullable MaybeProxyMeasure measureProxy) {
        this.typeProxy = typeProxy;
        this.measureProxy = measureProxy;
    }

    public void setNamespace(Namespace ns) {
        typeProxy.obj().setNamespace(ns);
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
    public Optional<MaybeProxyMeasure> measureProxy() {
        return Optional.ofNullable(measureProxy);
    }
}
