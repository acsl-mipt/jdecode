package ru.mipt.acsl.decode.model.types;

import ru.mipt.acsl.decode.model.HasNamespace;
import ru.mipt.acsl.decode.model.MayHaveAlias;
import ru.mipt.acsl.decode.model.Referenceable;
import ru.mipt.acsl.decode.model.ReferenceableVisitor;
import ru.mipt.acsl.decode.model.naming.ElementName;
import ru.mipt.acsl.decode.model.naming.Fqn;
import ru.mipt.acsl.decode.model.registry.Language;

import java.util.*;

/**
 * Created by metadeus on 06.06.16.
 */
public interface DecodeType extends Referenceable, HasNamespace, MayHaveAlias {

    default Map<Language, String> info() {
        return alias()
                .map(Alias::info)
                .orElseGet(HashMap::new);
    }

    @Override
    Optional<Alias> alias();

    String systemName();

    List<ElementName> typeParameters();

    default boolean isGeneric() {
        return !typeParameters().isEmpty();
    }

    default String nameOrSystemName() {
        return alias()
                .map(a -> a.name().mangledNameString())
                .orElse(systemName());
    }

    default Optional<Fqn> fqn() {
        return alias()
                .map(a -> {
                    List<ElementName> parts = new ArrayList<>(namespace().fqn().getParts());
                    parts.add(a.name());
                    return Fqn.newInstance(parts);
                });
    }

    default boolean isUnit() {
        throw new RuntimeException("not implemented");
    }

    default boolean isArray() {
        throw new RuntimeException("not implemented");
    }

    default boolean isNative() {
        return this instanceof NativeType;
    }

    default boolean isOrType() {
        return fqn().map(f -> f.equals(Fqn.OR)).orElse(false);
    }

    default boolean isOptionType() {
        return fqn().map(f -> f.equals(Fqn.OPTION)).orElse(false);
    }

    default boolean isVaruintType() {
        return fqn().map(f -> f.equals(Fqn.VARUINT)).orElse(false);
    }

    default Optional<ElementName> nameOption() {
        return alias().map(Alias::name);
    }

    @Override
    default <T> T accept(ReferenceableVisitor<T> visitor) {
        return visitor.visit(this);
    }

}
