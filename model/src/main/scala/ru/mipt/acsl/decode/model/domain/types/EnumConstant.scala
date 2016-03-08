package ru.mipt.acsl.decode.model.domain.types

import ru.mipt.acsl.decode.model.domain.HasOptionInfo
import ru.mipt.acsl.decode.model.domain.naming.ElementName

/**
  * Created by metadeus on 08.03.16.
  */
trait EnumConstant extends HasOptionInfo {
  def name: ElementName

  def value: String
}
