package ru.mipt.acsl.decode.model;

import ru.mipt.acsl.decode.model.naming.Namespace;

/**
 * Created by metadeus on 11.06.16.
 */
public interface HasNamespace {

    Namespace namespace();

    void setNamespace(Namespace namespace);

}
