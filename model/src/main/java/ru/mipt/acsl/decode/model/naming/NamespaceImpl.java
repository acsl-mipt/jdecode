package ru.mipt.acsl.decode.model.naming;

import org.jetbrains.annotations.Nullable;
import ru.mipt.acsl.decode.model.Referenceable;
import ru.mipt.acsl.decode.model.registry.Language;
import ru.mipt.acsl.decode.model.types.Alias;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Created by metadeus on 06.06.16.
 */
public class NamespaceImpl implements Namespace {

    private Alias.NsNs alias;
    private Namespace parent;
    private List<Referenceable> objects;

    NamespaceImpl(Alias.NsNs alias, @Nullable Namespace parent, List<Referenceable> objects) {
        this.alias = alias;
        this.parent = parent;
        this.objects = objects;
    }

    @Override
    public Map<Language, String> info() {
        return alias.info();
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
    public Alias.NsNs alias() {
        return alias;
    }

    @Override
    public Optional<Namespace> parent() {
        return Optional.ofNullable(parent);
    }

    @Override
    public void parent(@Nullable Namespace ns) {
        this.parent = ns;
    }
}
