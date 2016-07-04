package ru.mipt.acsl.decode.model.types;

import org.jetbrains.annotations.Nullable;
import ru.mipt.acsl.decode.model.Referenceable;
import ru.mipt.acsl.decode.model.naming.ElementName;
import ru.mipt.acsl.decode.model.naming.Namespace;
import ru.mipt.acsl.decode.model.proxy.MaybeProxyEnumType;
import ru.mipt.acsl.decode.model.proxy.MaybeTypeProxy;

import java.util.List;
import java.util.Optional;

/**
 * Created by metadeus on 07.06.16.
 */
public class EnumTypeImpl implements EnumType {

    private final MaybeProxyEnumOrTypeMeasure extendsOrBaseTypeProxy;
    @Nullable
    private final Alias.NsType alias;
    private final boolean isFinal;
    private final List<ElementName> typeParameters;
    private Namespace namespace;
    private List<Referenceable> objects;

    EnumTypeImpl(@Nullable Alias.NsType alias, Namespace namespace,
                 MaybeProxyEnumOrTypeMeasure extendsOrBaseTypeProxy,
                 List<Referenceable> objects, boolean isFinal,
                 List<ElementName> typeParameters) {
        this.alias = alias;
        this.namespace = namespace;
        this.extendsOrBaseTypeProxy = extendsOrBaseTypeProxy;
        this.objects = objects;
        this.isFinal = isFinal;
        this.typeParameters = typeParameters;
    }

    public Optional<MaybeProxyEnumType> extendsTypeProxy() {
        return extendsOrBaseTypeProxy().maybeProxyEnum();
    }

    @Override
    public boolean isFinal() {
        return isFinal;
    }

    @Override
    public Optional<Alias> alias() {
        return Optional.ofNullable(alias);
    }

    @Override
    public Optional<EnumType> extendsTypeOption() {
        return extendsTypeProxy().map(MaybeProxyEnumType::obj);
    }

    @Override
    public MaybeProxyEnumOrTypeMeasure extendsOrBaseTypeProxy() {
        return extendsOrBaseTypeProxy;
    }

    public MaybeTypeProxy baseTypeProxy() {
        Optional<MaybeProxyEnumType> anEnum = extendsOrBaseTypeProxy.maybeProxyEnum();
        return anEnum.isPresent() ? anEnum.get() : extendsOrBaseTypeProxy.typeMeasure().get().typeProxy(); // must not be null
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
    public Namespace namespace() {
        return namespace;
    }

    @Override
    public void setNamespace(Namespace ns) {
        this.namespace = ns;
    }

    @Override
    public List<ElementName> typeParameters() {
        return typeParameters;
    }

    @Override
    public String toString() {
        return String.format("%s{alias = %s, namespace = %s, extendsOrBaseTypeProxy = %s," +
                " objects = %d items, isFinal = %s, typeParameters = %s}", getClass(), alias, namespace,
                extendsOrBaseTypeProxy, objects.size(), isFinal, typeParameters);
    }

}
