package ru.mipt.acsl.decode.model.component;

import org.jetbrains.annotations.Nullable;
import ru.mipt.acsl.decode.model.Referenceable;
import ru.mipt.acsl.decode.model.component.Component;
import ru.mipt.acsl.decode.model.naming.Namespace;
import ru.mipt.acsl.decode.model.proxy.MaybeProxyCompanion;
import ru.mipt.acsl.decode.model.types.Alias;

import java.util.List;
import java.util.Optional;

/**
 * Created by metadeus on 11.06.16.
 */
class ComponentImpl implements Component {

    private final Alias.NsComponent alias;
    private Namespace namspace;
    @Nullable
    private final Integer id;
    @Nullable
    private final MaybeProxyCompanion.Struct baseTypeProxy;
    private List<Referenceable> objects;

    ComponentImpl(Alias.NsComponent alias, Namespace namespace, @Nullable Integer id,
                  @Nullable  MaybeProxyCompanion.Struct baseTypeProxy,
                  List<Referenceable> objects) {
        this.alias = alias;
        this.namspace = namespace;
        this.id = id;
        this.baseTypeProxy = baseTypeProxy;
        this.objects = objects;
    }

    @Override
    public List<Referenceable> objects() {
        return objects;
    }

    @Override
    public void setObjects(List<Referenceable> objects) {
        this.objects = objects;
    }

    @Override
    public Optional<Integer> id() {
        return Optional.ofNullable(id);
    }

    @Override
    public Namespace namespace() {
        return namspace;
    }

    @Override
    public Optional<MaybeProxyCompanion.Struct> baseTypeProxy() {
        return Optional.ofNullable(baseTypeProxy);
    }

    @Override
    public void setNamespace(Namespace ns) {
        this.namspace = ns;
    }

    @Override
    public Alias.NsComponent alias() {
        return alias;
    }

}
