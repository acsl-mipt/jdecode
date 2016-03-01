package ru.mipt.acsl.decode.parser;

import javax.swing.Icon;

import com.intellij.openapi.fileTypes.LanguageFileType;

/**
 * @author Artem Shein
 */
public final class DecodeFileType extends LanguageFileType
{
    public static final DecodeFileType INSTANCE = new DecodeFileType();
    private DecodeFileType() {
        super(DecodeLanguage.INSTANCE);
    }
    @Override public String getName() {
        return "Decode FILE";
    }

    @Override
    public String getDescription() {
        return "Decode device interface description";
    }

    @Override
    public String getDefaultExtension()
    {
        return "decode";
    }

    @Override
    public Icon getIcon()
    {
        return DecodeIcons.FILE;
    }
}
