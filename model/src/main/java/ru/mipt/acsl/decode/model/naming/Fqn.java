package ru.mipt.acsl.decode.model.naming;

import com.google.common.collect.Lists;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by metadeus on 06.06.16.
 */
public interface Fqn {

    Fqn DECODE_NAMESPACE = Fqn.newInstance("decode");
    Fqn RANGE = Fqn.newInstance(DECODE_NAMESPACE, ElementName.newInstanceFromMangledName("range"));
    Fqn VARUINT = Fqn.newInstance(DECODE_NAMESPACE, ElementName.newInstanceFromMangledName("varuint"));
    Fqn ARRAY = Fqn.newInstance(DECODE_NAMESPACE, ElementName.newInstanceFromMangledName("array"));
    Fqn UNIT = Fqn.newInstance(DECODE_NAMESPACE, ElementName.newInstanceFromMangledName("unit"));
    Fqn OPTION = Fqn.newInstance(DECODE_NAMESPACE, ElementName.newInstanceFromMangledName("option"));
    Fqn OR = Fqn.newInstance(DECODE_NAMESPACE, ElementName.newInstanceFromMangledName("or"));

    static Fqn newInstance(Fqn fqn, ElementName last) {
        List<ElementName> parts = Lists.newArrayList(fqn.getParts());
        parts.add(last);
        return Fqn.newInstance(parts);
    }

    static Fqn newInstance(String sourceText) {
        return newInstance(Stream.of(sourceText.split(Pattern.quote(".")))
                .map(ElementName::newInstanceFromSourceName).collect(Collectors.toList()));
    }

    static Fqn newInstance(List<ElementName> parts) {
        return new FqnImpl(parts);
    }

    List<ElementName> getParts();

    default String mangledNameString() {
        return getParts().stream().map(ElementName::mangledNameString).collect(Collectors.joining("."));
    }

    default ElementName last() {
        return getParts().get(getParts().size() - 1);
    }

    Fqn copyDropLast();

    default Integer size() {
        return getParts().size();
    }

    default Boolean isEmpty() {
        return getParts().isEmpty();
    }

}
