package ru.mipt.acsl.decode.model.types;

import org.jetbrains.annotations.Nullable;
import ru.mipt.acsl.decode.model.Referenceable;
import ru.mipt.acsl.decode.model.ReferenceableVisitor;
import ru.mipt.acsl.decode.model.naming.Container;
import ru.mipt.acsl.decode.model.naming.ElementName;
import ru.mipt.acsl.decode.model.naming.Namespace;
import ru.mipt.acsl.decode.model.proxy.MaybeProxy;

import java.util.*;

/**
 * Created by metadeus on 07.06.16.
 */
public interface EnumType extends DecodeType, Container {

    static EnumType newInstance(@Nullable Alias.NsType alias, Namespace namespace,
                                MaybeProxyEnumOrTypeMeasure extendsOrBaseTypeProxy,
                                List<Referenceable> objects, boolean isFinal,
                                List<ElementName> typeParameters) {
        return new EnumTypeImpl(alias, namespace, extendsOrBaseTypeProxy, objects, isFinal, typeParameters);
    }

    boolean isFinal();

    default List<EnumConstant> constants() {
        return filterByClass(EnumConstant.class);
    }

    default Optional<EnumType> extendsTypeOption() {
        return extendsOrBaseTypeProxy().maybeProxyEnum().map(MaybeProxy.Enum::obj);
    }

    default Optional<DecodeType> baseTypeOption() {
        return extendsOrBaseTypeProxy().typeMeasure().map(TypeMeasure::t);
    }

    MaybeProxyEnumOrTypeMeasure extendsOrBaseTypeProxy();

    default DecodeType extendsOrBaseType() {
        return extendsTypeOption()
                .map(DecodeType.class::cast)
                .orElseGet(this::baseType);
    }

    default EnumTypeOrTypeMeasure eitherExtendsOrBaseType() {
        Optional<EnumType> enumType = extendsTypeOption();
        return enumType.isPresent()
                ? new EnumTypeOrTypeMeasure(enumType.get())
                : new EnumTypeOrTypeMeasure(extendsOrBaseTypeProxy().typeMeasure().get()); // must not be null
    }

    default DecodeType baseType() {
        Optional<EnumType> enumType = extendsTypeOption();
        return enumType.isPresent() ? enumType.get() : baseTypeOption().get(); // must not be null
    }

    default String systemName() {
        return "EnumType@" + hashCode();
    }

    default Set<EnumConstant> allConstants() {
        Set<EnumConstant> result = new HashSet<>(constants());
        result.addAll(extendsTypeOption().map(EnumType::allConstants).orElseGet(Collections::emptySet));
        return result;
    }

    default void accept(ReferenceableVisitor visitor) {
        visitor.visit(this);
    }

}
