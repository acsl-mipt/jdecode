package ru.mipt.acsl.decode.model;

/**
 * Created by metadeus on 06.06.16.
 */
public interface Referenceable {

    void accept(ReferenceableVisitor visitor);

}
