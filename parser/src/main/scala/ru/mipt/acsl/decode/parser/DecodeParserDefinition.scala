package ru.mipt.acsl.decode.parser

import com.intellij.lang.ASTNode
import com.intellij.lang.Language
import com.intellij.lang.ParserDefinition
import com.intellij.lang.ParserDefinition.SpaceRequirements
import com.intellij.lang.PsiParser
import com.intellij.lexer.Lexer
import com.intellij.openapi.project.Project
import com.intellij.psi._
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet
import ru.mipt.acsl.decode.parser.psi.{DecodeExpr, DecodeTypes}

/**
 * @author Artem Shein
 */
object DecodeParserDefinition {
    val file: IFileElementType = new IFileElementType(Language.findInstance(classOf[DecodeLanguage]))
    val whitespaces = TokenSet.create(TokenType.WHITE_SPACE, DecodeTypes.COMMENT, DecodeTypes.MULTILINE_COMMENT)
    val comments = TokenSet.create(DecodeTypes.COMMENT, DecodeTypes.MULTILINE_COMMENT)
}

class DecodeParserDefinition extends ParserDefinition {
    import DecodeParserDefinition._

    override def createLexer(project: Project): Lexer = new DecodeLexerAdapter

    override def createParser(project: Project): PsiParser = new DecodeParser

    override def getFileNodeType: IFileElementType = file

    override def getWhitespaceTokens: TokenSet = whitespaces

    override def getCommentTokens: TokenSet = comments

    override def getStringLiteralElements: TokenSet = TokenSet.EMPTY

    override def createElement(node: ASTNode): PsiElement = DecodeTypes.Factory.createElement(node)

    override def createFile(viewProvider: FileViewProvider): PsiFile = new DecodeFile(viewProvider)

    override def spaceExistanceTypeBetweenTokens(astNode: ASTNode, astNode1: ASTNode): SpaceRequirements =
        if (astNode.getPsi.isInstanceOf[DecodeExpr] && astNode1.getPsi.isInstanceOf[DecodeExpr])
            SpaceRequirements.MUST_LINE_BREAK
        else
            SpaceRequirements.MAY
}
