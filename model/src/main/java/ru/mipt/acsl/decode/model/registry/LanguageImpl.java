package ru.mipt.acsl.decode.model.registry;

/**
 * Created by metadeus on 06.06.16.
 */
public class LanguageImpl implements Language {

    private final String code;

    @Override
    public String code() {
        return code;
    }

    LanguageImpl(String code) {
        this.code = code;
    }
}
