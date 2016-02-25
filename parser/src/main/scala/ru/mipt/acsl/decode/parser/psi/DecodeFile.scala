package ru.mipt.acsl.decode.parser.psi

import com.intellij.openapi.fileTypes.FileType
import ru.mipt.acsl.decode.parser.{DecodeFileType, DecodeLanguage}
import com.intellij.extapi.psi.PsiFileBase
import com.intellij.psi.FileViewProvider

/**
 * @author Artem Shein
 */
abstract class DecodeFile(viewProvider: FileViewProvider) extends PsiFileBase(viewProvider, DecodeLanguage.instance)
{
  override def getFileType: FileType = DecodeFileType.instance

  override def toString: String = "Device components definition file"
}
