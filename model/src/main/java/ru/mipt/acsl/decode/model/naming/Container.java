package ru.mipt.acsl.decode.model.naming;

import ru.mipt.acsl.decode.model.Referenceable;
import ru.mipt.acsl.decode.model.ReferenceableVisitor;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by metadeus on 06.06.16.
 */
public interface Container extends Referenceable {

    default <T extends Referenceable> List<T> filterByClass(Class<T> cls) {
        return objects().stream().flatMap(o -> cls.isInstance(o) ? Stream.of(cls.cast(o)) : Stream.empty())
                .collect(Collectors.toList());
    }

    List<Referenceable> objects();

    void objects(List<Referenceable> objects);

    default void accept(ReferenceableVisitor visitor) {
        visitor.visit(this);
    }

}
