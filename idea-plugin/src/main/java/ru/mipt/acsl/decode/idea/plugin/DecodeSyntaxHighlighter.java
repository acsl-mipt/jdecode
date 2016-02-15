package ru.mipt.acsl.decode.idea.plugin;

import ru.mipt.acsl.decode.parser.DecodeLexerAdapter;
import ru.mipt.acsl.decode.parser.psi.DecodeTypes;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Artem Shein
 */
public class DecodeSyntaxHighlighter extends SyntaxHighlighterBase
{
    private static final TextAttributesKey KEYWORD = TextAttributesKey.createTextAttributesKey("DECODE_KEYWORD",
            DefaultLanguageHighlighterColors.KEYWORD);
    private static final TextAttributesKey ELEMENT_NAME = TextAttributesKey.createTextAttributesKey("DECODE_ELEMENT_NAME",
            DefaultLanguageHighlighterColors.IDENTIFIER);
    private static final TextAttributesKey BRACE = TextAttributesKey.createTextAttributesKey("DECODE_BRACE",
            DefaultLanguageHighlighterColors.BRACES);
    private static final TextAttributesKey SLASH = TextAttributesKey.createTextAttributesKey("DECODE_SLASH",
            DefaultLanguageHighlighterColors.MARKUP_TAG);
    private static final TextAttributesKey BRACKET = TextAttributesKey.createTextAttributesKey("DECODE_BRACKET",
            DefaultLanguageHighlighterColors.BRACKETS);
    private static final TextAttributesKey DOT = TextAttributesKey.createTextAttributesKey("DECODE_DOT",
            DefaultLanguageHighlighterColors.DOT);
    private static final TextAttributesKey STAR = TextAttributesKey.createTextAttributesKey("DECODE_STAR",
            DefaultLanguageHighlighterColors.OPERATION_SIGN);
    private static final TextAttributesKey PAREN = TextAttributesKey.createTextAttributesKey("DECODE_PAREN",
            DefaultLanguageHighlighterColors.PARENTHESES);
    private static final TextAttributesKey COMMA = TextAttributesKey.createTextAttributesKey("DECODE_COMMA",
            DefaultLanguageHighlighterColors.COMMA);
    private static final TextAttributesKey LINE_COMMENT = TextAttributesKey.createTextAttributesKey("DECODE_LINE_COMMENT",
            DefaultLanguageHighlighterColors.LINE_COMMENT);
    private static final TextAttributesKey BLOCK_COMMENT = TextAttributesKey.createTextAttributesKey("DECODE_BLOCK_COMMENT",
            DefaultLanguageHighlighterColors.BLOCK_COMMENT);
    private static final TextAttributesKey ELEMENT_ID = TextAttributesKey.createTextAttributesKey("DECODE_ELEMENT_ID",
            DefaultLanguageHighlighterColors.IDENTIFIER);
    private static final TextAttributesKey NON_NEGATIVE_NUMBER = TextAttributesKey.createTextAttributesKey("DECODE_NON_NEGATIVE_NUMBER",
            DefaultLanguageHighlighterColors.NUMBER);
    private static final TextAttributesKey BAD_CHARACTER = TextAttributesKey.createTextAttributesKey("DECODE_BAD_CHARACTER",
            DefaultLanguageHighlighterColors.INVALID_STRING_ESCAPE);
    private static final TextAttributesKey STRING = TextAttributesKey.createTextAttributesKey("DECODE_STRING",
            DefaultLanguageHighlighterColors.STRING);

    private static final TextAttributesKey[] EMPTY_KEYS = new TextAttributesKey[0];
    private static final TextAttributesKey[] KEYWORD_KEYS = {KEYWORD};
    private static final TextAttributesKey[] BAD_CHARACTER_KEYS = {BAD_CHARACTER};
    private static final TextAttributesKey[] ELEMENT_NAME_KEYS = {ELEMENT_NAME};
    private static final TextAttributesKey[] ELEMENT_ID_KEYS = {ELEMENT_ID};
    private static final TextAttributesKey[] BRACES_KEYS = {BRACE};
    private static final TextAttributesKey[] PARENS_KEYS = {PAREN};
    private static final TextAttributesKey[] COMMA_KEYS = {COMMA};
    private static final TextAttributesKey[] LINE_COMMENT_KEYS ={LINE_COMMENT};
    private static final TextAttributesKey[] BLOCK_COMMENT_KEYS ={BLOCK_COMMENT};
    private static final TextAttributesKey[] BRACKETS_KEYS = {BRACKET};
    private static final TextAttributesKey[] SLASH_KEYS = {SLASH};
    private static final TextAttributesKey[] DOT_KEYS = {DOT};
    private static final Set<IElementType> KEYWORD_TOKENS = new HashSet<IElementType>(){{
        Collections.addAll(this, DecodeTypes.NAMESPACE, DecodeTypes.COMPONENT, DecodeTypes.COMMAND, DecodeTypes.DYNAMIC,
                DecodeTypes.EVENT, DecodeTypes.MESSAGE, DecodeTypes.STATUS, DecodeTypes.PARAMETER, DecodeTypes.INFO,
                DecodeTypes.ARRAY, DecodeTypes.BASE_TYPE, DecodeTypes.BOOL, DecodeTypes.ENUM, DecodeTypes.BER,
                DecodeTypes.FLOAT, DecodeTypes.INT, DecodeTypes.UINT, DecodeTypes.STRUCT, DecodeTypes.UNIT_TOKEN,
                DecodeTypes.TYPE_KEYWORD, DecodeTypes.SUBCOMPONENT, DecodeTypes.DISPLAY, DecodeTypes.PLACEMENT,
                DecodeTypes.BEFORE, DecodeTypes.AFTER, DecodeTypes.COLON, DecodeTypes.ALIAS, DecodeTypes.WITH,
                DecodeTypes.PARAMETERS, DecodeTypes.IMPORT, DecodeTypes.AS, DecodeTypes.DEFAULT, DecodeTypes.LANGUAGE,
                DecodeTypes.VAR);
    }};
    private static final TextAttributesKey[] NON_NEGATIVE_NUMBER_KEYS = {NON_NEGATIVE_NUMBER};
    private static final TextAttributesKey[] STAR_KEYS = {STAR};
    private static final TextAttributesKey[] STRING_KEYS = {STRING};

    @NotNull
    @Override
    public Lexer getHighlightingLexer()
    {
        return new DecodeLexerAdapter();
    }

    @NotNull
    @Override
    public TextAttributesKey[] getTokenHighlights(IElementType tokenType)
    {
        if (KEYWORD_TOKENS.contains(tokenType))
        {
            return KEYWORD_KEYS;
        }
        else if (tokenType.equals(DecodeTypes.ELEMENT_NAME_TOKEN) || tokenType.equals(DecodeTypes.ESCAPED_NAME))
        {
            return ELEMENT_NAME_KEYS;
        }
        else if (tokenType.equals(DecodeTypes.ELEMENT_ID))
        {
            return ELEMENT_ID_KEYS;
        }
        else if (tokenType.equals(DecodeTypes.NON_NEGATIVE_NUMBER))
        {
            return NON_NEGATIVE_NUMBER_KEYS;
        }
        else if (tokenType.equals(DecodeTypes.LEFT_BRACE) || tokenType.equals(DecodeTypes.RIGHT_BRACE))
        {
            return BRACES_KEYS;
        }
        else if (tokenType.equals(DecodeTypes.LEFT_PAREN) || tokenType.equals(DecodeTypes.RIGHT_PAREN))
        {
            return PARENS_KEYS;
        }
        else if (tokenType.equals(DecodeTypes.LEFT_BRACKET) || tokenType.equals(DecodeTypes.RIGHT_BRACKET))
        {
            return BRACKETS_KEYS;
        }
        else if (tokenType.equals(DecodeTypes.COMMA))
        {
            return COMMA_KEYS;
        }
        else if (tokenType.equals(DecodeTypes.SLASH))
        {
            return SLASH_KEYS;
        }
        else if (tokenType.equals(DecodeTypes.DOT))
        {
            return DOT_KEYS;
        }
        else if (tokenType.equals(DecodeTypes.STAR))
        {
            return STAR_KEYS;
        }
        else if (tokenType.equals(DecodeTypes.COMMENT))
        {
            return LINE_COMMENT_KEYS;
        }
        else if (tokenType.equals(DecodeTypes.MULTILINE_COMMENT))
        {
            return BLOCK_COMMENT_KEYS;
        }
        else if (tokenType.equals(DecodeTypes.STRING_VALUE) || tokenType.equals(DecodeTypes.STRING)
                || tokenType.equals(DecodeTypes.STRING_UNARY_QUOTES))
        {
            return STRING_KEYS;
        }
        else if (tokenType.equals(TokenType.BAD_CHARACTER))
        {
            return BAD_CHARACTER_KEYS;
        }
        else
        {
            return EMPTY_KEYS;
        }
    }
}
