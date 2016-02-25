package ru.mipt.acsl.decode.parser.psi

import ru.mipt.acsl.decode.parser.DecodeLanguage
import com.intellij.psi.tree.IElementType

/**
 * @author Artem Shein
 */
class DecodeTokenType(debugName: String) extends IElementType(debugName, DecodeLanguage.instance)
{
    override def toString: String = "DecodeTokenType." + super.toString
}
