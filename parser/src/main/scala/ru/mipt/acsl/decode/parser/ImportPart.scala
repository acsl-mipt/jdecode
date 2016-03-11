package ru.mipt.acsl.decode.parser

import ru.mipt.acsl.decode.model.domain.naming.ElementName

/**
  * @author Artem Shein
  */
private trait ImportPart {
  def alias: String

  def originalName: ElementName
}
