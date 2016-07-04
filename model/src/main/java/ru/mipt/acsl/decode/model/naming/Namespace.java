package ru.mipt.acsl.decode.model.naming;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.Nullable;
import ru.mipt.acsl.decode.model.*;
import ru.mipt.acsl.decode.model.component.Component;
import ru.mipt.acsl.decode.model.proxy.MaybeProxyNamespace;
import ru.mipt.acsl.decode.model.registry.Language;
import ru.mipt.acsl.decode.model.registry.Measure;
import ru.mipt.acsl.decode.model.types.Alias;
import ru.mipt.acsl.decode.model.types.Const;
import ru.mipt.acsl.decode.model.types.DecodeType;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by metadeus on 06.06.16.
 */
public interface Namespace extends Container, HasName, HasInfo, HasAlias {

    ElementName ROOT_NAME = ElementName.newInstanceFromMangledName("%root");

    static Namespace newInstance(Alias.NsNs alias, List<Referenceable> objects) {
        return new NamespaceImpl(alias, objects);
    }

    static Namespace newInstanceRoot() {
        Alias.NsNs alias = new Alias.NsNs(ROOT_NAME, Collections.emptyMap(), null, null);
        alias.obj(MaybeProxyNamespace.newInstance(Namespace.newInstance(alias, Lists.newArrayList(alias))));
        alias.parent(alias.obj().obj());
        return alias.obj().obj();
    }

    static Namespace newInstance(Fqn fqn, Namespace rootNamespace, Map<Language, String> info) {
        Namespace currentNamespace = rootNamespace;
        int size = fqn.size();
        int index = 0;
        for (ElementName part : fqn.getParts()) {
            Alias.NsNs alias = new Alias.NsNs(part, index == size - 1 ? info : Collections.emptyMap(), currentNamespace, null);
            Namespace ns = Namespace.newInstance(alias, new ArrayList<>());
            alias.obj(MaybeProxyNamespace.newInstance(ns));
            List<Referenceable> objects = currentNamespace.objects();
            objects.add(ns);
            objects.add(alias);
            currentNamespace = ns;
        }
        return currentNamespace;
    }

    @Override
    Alias.NsNs alias();

    /**
     * Get measures of the current namespace
     *
     * @return List of [[Measure]]
     */
    default List<Measure> measures() {
        return filterByClass(Measure.class);
    }

    /**
     * Get types of current namespace
     *
     * @return List of [[DecodeType]]
     */
    default List<DecodeType> types() {
        return filterByClass(DecodeType.class);
    }

    /**
     * Get subset of current namespaces
     *
     * @return Seq of [[Namespace]]
     */
    default List<Namespace> subNamespaces() {
        return filterByClass(Namespace.class);
    }

    /**
     * Get parent [[Namespace]] of current namespace
     *
     * @return Parent [[Namespace]] if it defined, otherwise - None
     */
    Optional<Namespace> parent();

    void setParent(@Nullable Namespace ns);

    default boolean isRoot() {
        return name().equals(ROOT_NAME);
    }

    default ElementName name() {
        return alias().name();
    }

    default Map<Language, String> getInfo() {
        return alias().info();
    }

    /**
     * Get components of current namespace
     *
     * @return Seq of [[Component]]
     */
    default List<Component> components() {
        return filterByClass(Component.class);
    }

    default Optional<Alias> alias(ElementName name) {
        List<Alias> aliases = objects().stream()
                .flatMap(a -> a instanceof Alias && ((Alias)a).name().equals(name)
                        ? Stream.of((Alias) a)
                        : Stream.empty())
                .collect(Collectors.toList());
        return aliases.size() == 1 ? Optional.of(aliases.get(0)) : Optional.empty();
    }

    default List<Alias> aliases() {
        return filterByClass(Alias.class);
    }

    default List<Const> consts() {
        return filterByClass(Const.class);
    }

    default Namespace rootNamespace() {
        return parent().map(Namespace::rootNamespace).orElse(this);
    }

    /**
     * Get all components of current namespace
     *
     * @return Seq of [[Component]]
     */
    default List<Component> allComponents() {
        List<Component> result = subNamespaces().stream().flatMap(n -> n.allComponents().stream())
                .collect(Collectors.toList());
        result.addAll(components());
        return result;
    }

    /**
     * Get all namespaces
     *
     * @return Seq of [[Namespace]]
     */
    default List<Namespace> allNamespaces() {
        List<Namespace> result = subNamespaces().stream().flatMap(n -> n.allNamespaces().stream())
                .collect(Collectors.toList());
        result.add(this);
        return result;
    }

    /**
     * Get fully qualified name
     *
     * @return [[Fqn]]
     */
    default Fqn fqn() {
        ArrayList<ElementName> parts = new ArrayList<>();
        Namespace currentNamespace = this;
        while (currentNamespace.parent().isPresent()) {
            Namespace parent = currentNamespace.parent().get();
            if (!currentNamespace.isRoot()) {
                parts.add(currentNamespace.name());
            }
            currentNamespace = parent;
        }
        if (!currentNamespace.isRoot()) {
            parts.add(currentNamespace.name());
        }
        Collections.reverse(parts);
        return Fqn.newInstance(parts);
    }

    @Override
    default <T> T accept(ContainerVisitor<T> visitor) {
        return visitor.visit(this);
    }

}
