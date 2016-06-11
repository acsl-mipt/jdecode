package ru.mipt.acsl.decode.model;

/**
 * Created by metadeus on 06.06.16.
 */
public interface TmParameter extends HasInfo, Referenceable {

    default <T> T accept(ReferenceableVisitor<T> visitor) {
        return visitor.visit(this);
    }

}
