package ru.mipt.acsl.decode.parser.psi

import ru.mipt.acsl.decode.parser.DecodeLanguage
import com.intellij.psi.tree.IElementType

/**
 * @author Artem Shein
 */
class DecodeElementType(debugName: String) extends IElementType(debugName, DecodeLanguage.INSTANCE)
