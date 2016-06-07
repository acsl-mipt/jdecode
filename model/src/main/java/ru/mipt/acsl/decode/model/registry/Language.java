package ru.mipt.acsl.decode.model.registry;

/**
 * Created by metadeus on 06.06.16.
 */
public interface Language {

    static Language newInstance(String code) {
        return new LanguageImpl(code);
    }

    String code();

}
