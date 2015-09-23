package ru.mipt.acsl.decode.parser;

import com.intellij.openapi.fileTypes.LanguageFileType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @author Artem Shein
 */
public final class DecodeFileType extends LanguageFileType
{
    public static final DecodeFileType INSTANCE = new DecodeFileType();

    /**
     * Creates a language file type.
     */
    private DecodeFileType()
    {
        super(DecodeLanguage.INSTANCE);
    }

    @NotNull
    @Override
    public String getName()
    {
        return "Decode file";
    }

    @NotNull
    @Override
    public String getDescription()
    {
        return "Decode device interface description";
    }

    @NotNull
    @Override
    public String getDefaultExtension()
    {
        return "decode";
    }

    @Nullable
    @Override
    public Icon getIcon()
    {
        return DecodeIcons.FILE;
    }
}
