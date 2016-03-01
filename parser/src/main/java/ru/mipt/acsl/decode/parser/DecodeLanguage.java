package ru.mipt.acsl.decode.parser;

import com.intellij.lang.Language;

/**
 * Created by metadeus on 02.03.16.
 */
public class DecodeLanguage extends Language {
    public static final DecodeLanguage INSTANCE = new DecodeLanguage();

    private DecodeLanguage() {
        super("Decode");
    }
}
