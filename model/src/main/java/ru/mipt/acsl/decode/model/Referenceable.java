package ru.mipt.acsl.decode.model;

/**
 * Created by metadeus on 06.06.16.
 */
public interface Referenceable {

    <T> T accept(ReferenceableVisitor<T> visitor);

}
