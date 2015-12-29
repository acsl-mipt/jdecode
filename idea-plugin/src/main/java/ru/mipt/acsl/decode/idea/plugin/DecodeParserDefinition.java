package ru.mipt.acsl.decode.idea.plugin;

import ru.mipt.acsl.decode.parser.DecodeLanguage;
import ru.mipt.acsl.decode.parser.DecodeLexerAdapter;
import ru.mipt.acsl.decode.parser.idea.DecodeParser;
import ru.mipt.acsl.decode.parser.psi.DecodeFile;
import com.intellij.lang.ASTNode;
import com.intellij.lang.Language;
import com.intellij.lang.ParserDefinition;
import com.intellij.lang.PsiParser;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.TokenSet;
import ru.mipt.acsl.decode.parser.psi.DecodeTypes;
import org.jetbrains.annotations.NotNull;

/**
 * @author Artem Shein
 */
public class DecodeParserDefinition implements ParserDefinition
{
    private static final IFileElementType FILE = new IFileElementType(
            Language.findInstance(DecodeLanguage.class));
    private static final TokenSet WHITESPACES = TokenSet.create(TokenType.WHITE_SPACE, DecodeTypes.COMMENT,
            DecodeTypes.MULTILINE_COMMENT);
    private static final TokenSet COMMENTS = TokenSet.create(DecodeTypes.COMMENT, DecodeTypes.MULTILINE_COMMENT);

    @NotNull
    @Override
    public Lexer createLexer(Project project)
    {
        return new DecodeLexerAdapter();
    }

    @Override
    public PsiParser createParser(Project project)
    {
        return new DecodeParser();
    }

    @Override
    public IFileElementType getFileNodeType()
    {
        return FILE;
    }

    @NotNull
    @Override
    public TokenSet getWhitespaceTokens()
    {
        return WHITESPACES;
    }

    @NotNull
    @Override
    public TokenSet getCommentTokens()
    {
        return COMMENTS;
    }

    @NotNull
    @Override
    public TokenSet getStringLiteralElements()
    {
        return TokenSet.EMPTY;
    }

    @NotNull
    @Override
    public PsiElement createElement(ASTNode node)
    {
        return DecodeTypes.Factory.createElement(node);
    }

    @Override
    public PsiFile createFile(FileViewProvider viewProvider)
    {
        return new DecodeFile(viewProvider);
    }

    @Override
    public SpaceRequirements spaceExistanceTypeBetweenTokens(ASTNode left, ASTNode right)
    {
        return SpaceRequirements.MAY;
    }
}
