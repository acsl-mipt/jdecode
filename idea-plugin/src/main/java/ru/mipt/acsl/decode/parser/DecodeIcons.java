package ru.mipt.acsl.decode.parser;

import com.intellij.openapi.util.IconLoader;

import javax.swing.*;

/**
 * @author Artem Shein
 */
public final class DecodeIcons
{
    public static final Icon FILE = IconLoader.getIcon("/ru/mipt/acsl/decode/idea/plugin/icons/decode_file_icon.png");

    // Deny instantiation
    private DecodeIcons()
    {
        throw new AssertionError();
    }
}
