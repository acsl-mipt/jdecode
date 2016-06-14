package ru.mipt.acsl.decode.model.types;

import org.jetbrains.annotations.Nullable;
import ru.mipt.acsl.decode.model.naming.ElementName;
import ru.mipt.acsl.decode.model.naming.Namespace;

import java.util.List;
import java.util.Optional;

/**
 * Created by metadeus on 07.06.16.
 */
public class ConstImpl implements Const {

    @Nullable
    private final Alias.NsConst alias;
    private Namespace namespace;
    private final String value;
    private final List<ElementName> typeParameters;

    ConstImpl(@Nullable Alias.NsConst alias, Namespace namespace, String value, List<ElementName> typeParameters) {
        this.alias = alias;
        this.namespace = namespace;
        this.value = value;
        this.typeParameters = typeParameters;
    }

    @Override
    public String systemName() { return value; }

    @Override
    public List<ElementName> typeParameters() {
        return typeParameters;
    }

    @Override
    public Namespace namespace() {
        return namespace;
    }

    @Override
    public void namespace(Namespace ns) {
        namespace = ns;
    }

    @Override
    public Optional<Alias> alias() {
        return Optional.ofNullable(alias);
    }

    @Override
    public String value() {
        return value;
    }

    @Override
    public String toString() {
        return String.format("%s{alias = %s, namespace = %s, value = %s}", getClass(), alias, namespace, value);
    }

}
