package ru.mipt.acsl.decode.parser

import com.intellij.openapi.fileTypes.LanguageFileType

import javax.swing.Icon

object DecodeFileType {
    val instance : DecodeFileType = new DecodeFileType()
}

/**
 * @author Artem Shein
 */
final class DecodeFileType extends LanguageFileType(DecodeLanguage.instance)
{
    override def getName: String = "Decode file"

    override def getDescription: String = "Decode device interface description"

    override def getDefaultExtension: String = "decode"

    override def getIcon: Icon = DecodeIcons.file
}
