package ru.mipt.acsl.decode.parser

import ru.mipt.acsl.decode.model.naming.ElementName

/**
  * @author Artem Shein
  */
private case class ImportPartNameAlias(originalName: ElementName, _alias: ElementName) extends ImportPart {
  def alias: String = _alias.mangledNameString()
}
