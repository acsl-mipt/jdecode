package ru.mipt.acsl.decode.model.types;

import ru.mipt.acsl.decode.model.Referenceable;
import ru.mipt.acsl.decode.model.naming.Container;
import ru.mipt.acsl.decode.model.naming.ElementName;
import ru.mipt.acsl.decode.model.registry.Language;

import java.util.Map;

/**
 * Created by metadeus on 06.06.16.
 */
abstract class AbstractAlias<P extends Container, O extends Referenceable> implements Alias<P, O> {

    private final ElementName name;
    private final Map<Language, String> info;
    private P parent;
    private O obj;

    AbstractAlias(ElementName name, Map<Language, String> info, P parent, O obj) {
        this.name = name;
        this.parent = parent;
        this.info = info;
        this.obj = obj;
    }

    @Override
    public ElementName name() {
        return name;
    }

    @Override
    public P parent() {
        return parent;
    }

    @Override
    public void parent(P parent) {
        this.parent = parent;
    }

    @Override
    public Map<Language, String> info() {
        return info;
    }

    @Override
    public O obj() {
        return obj;
    }

    @Override
    public void obj(O obj) {
        this.obj = obj;
    }

    @Override
    public String toString() {
        return String.format("%s{name = %s, info = %s, parent = %s, obj = %s}", getClass().getSimpleName(), name, info,
                parent, obj.getClass().getSimpleName());
    }

}
