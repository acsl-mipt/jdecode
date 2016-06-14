package ru.mipt.acsl.decode.model;

import ru.mipt.acsl.decode.model.types.Alias;

import java.util.Optional;

/**
 * Created by metadeus on 11.06.16.
 */
public interface MayHaveAlias {

    Optional<Alias> alias();

}
