package ru.mipt.acsl.decode.parser;

import com.intellij.lang.Language;

/**
 * @author Artem Shein
 */
public class DecodeLanguage extends Language
{
    public static final DecodeLanguage INSTANCE = new DecodeLanguage();

    private DecodeLanguage()
    {
        super("Decode");
    }
}
