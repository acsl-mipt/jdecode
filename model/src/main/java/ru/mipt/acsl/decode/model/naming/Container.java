package ru.mipt.acsl.decode.model.naming;

import ru.mipt.acsl.decode.model.ContainerVisitor;
import ru.mipt.acsl.decode.model.Referenceable;
import ru.mipt.acsl.decode.model.ReferenceableVisitor;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by metadeus on 06.06.16.
 */
public interface Container extends Referenceable {

    List<Referenceable> objects();

    void setObjects(List<Referenceable> objects);

    <T> T accept(ContainerVisitor<T> visitor);

    default <T> T accept(ReferenceableVisitor<T> visitor) {
        return visitor.visit(this);
    }

    default <T extends Referenceable> Stream<T> filterByClassStream(Class<T> cls) {
        return objects().stream().flatMap(o -> cls.isInstance(o) ? Stream.of(cls.cast(o)) : Stream.empty());
    }

    default <T extends Referenceable> List<T> filterByClass(Class<T> cls) {
        return filterByClassStream(cls).collect(Collectors.toList());
    }

    default <T extends Referenceable & HasName> Stream<T> filterByClassAndNameStream(Class<T> cls, ElementName name) {
        return objects().stream().flatMap(o -> cls.isInstance(o) && ((HasName) o).name().equals(name)
                ? Stream.of(cls.cast(o))
                : Stream.empty());
    }

}
