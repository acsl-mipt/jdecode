package ru.mipt.acsl.decode.model.types

import ru.mipt.acsl.decode.model._
import ru.mipt.acsl.decode.model.expr.ConstExpr
import ru.mipt.acsl.decode.model.naming.ElementName

/**
  * Created by metadeus on 08.03.16.
  */
trait EnumConstant extends HasNameAndInfo {

  def name: ElementName

  def value: ConstExpr

}

object EnumConstant {

  private class Impl(val name: ElementName, val value: ConstExpr, info: LocalizedString)
    extends AbstractHasInfo(info) with EnumConstant

  def apply(name: ElementName, value: ConstExpr, info: LocalizedString): EnumConstant =
    new Impl(name, value, info)
}
