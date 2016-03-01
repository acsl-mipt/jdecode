package ru.mipt.acsl.decode.parser.psi

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.psi.FileViewProvider
import ru.mipt.acsl.decode.parser.DecodeFileType
import ru.mipt.acsl.decode.parser.DecodeLanguage

/**
 * @author Artem Shein
 */
// FIXME: It's not abstract, but some WTF is here
abstract class DecodeFileWtf(viewProvider: FileViewProvider) extends PsiFileBase(viewProvider, DecodeLanguage.INSTANCE) {
    override def getFileType: FileType = DecodeFileType

    override def toString: String = "Device components definition file"
}
