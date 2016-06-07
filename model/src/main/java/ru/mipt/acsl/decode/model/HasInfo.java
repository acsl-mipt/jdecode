package ru.mipt.acsl.decode.model;

import ru.mipt.acsl.decode.model.registry.Language;

import java.util.Map;

/**
 * Created by metadeus on 06.06.16.
 */
public interface HasInfo {

    Map<Language, String> info();

}
