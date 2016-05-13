package ru.mipt.acsl.decode.parser

import ru.mipt.acsl.decode.model.naming.ElementName

/**
  * @author Artem Shein
  */
private case class ImportPartName(originalName: ElementName) extends ImportPart {
  def alias: String = originalName.asMangledString
}
