package ru.mipt.acsl.decode.parser;

import com.intellij.lexer.FlexAdapter;

/**
 * @author Artem Shein
 */
public class DecodeLexerAdapter extends FlexAdapter
{
    public DecodeLexerAdapter()
    {
        super(new _DecodeLexer());
    }
}
