package ru.mipt.acsl.decode.model.domain.types

import ru.mipt.acsl.decode.model.domain.{EnumConstant, HasBaseType}
import ru.mipt.acsl.decode.model.domain.proxy.MaybeProxy

trait EnumType extends DecodeType with HasBaseType {
  def isFinal: Boolean
  def extendsOrBaseType: Either[MaybeProxy[EnumType], MaybeProxy[DecodeType]]
  def extendsType: Option[MaybeProxy[EnumType]]
  def constants: Set[EnumConstant]
  def allConstants: Set[EnumConstant] = constants ++ extendsType.map(_.obj.allConstants).getOrElse(Set.empty)
}
