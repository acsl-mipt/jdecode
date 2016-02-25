package ru.mipt.acsl.decode.parser

import com.intellij.lexer.{FlexAdapter, Lexer}

/**
  * @author Artem Shein
  */
class DecodeLexerAdapter extends FlexAdapter(new _DecodeLexer())
