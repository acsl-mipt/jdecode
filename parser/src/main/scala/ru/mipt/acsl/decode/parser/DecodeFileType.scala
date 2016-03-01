package ru.mipt.acsl.decode.parser

import javax.swing.Icon

import com.intellij.openapi.fileTypes.LanguageFileType

/**
 * @author Artem Shein
 */
class DecodeFileType extends LanguageFileType(DecodeLanguage.INSTANCE) {
    override def getName: String = "Decode File"

    override def getDescription: String = "Decode device interface description"

    override def getDefaultExtension: String = "decode"

    override def getIcon: Icon = DecodeIcons.FILE
}

object DecodeFileType extends DecodeFileType