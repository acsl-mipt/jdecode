package ru.mipt.acsl.decode.model.types

import ru.mipt.acsl.decode.model._
import ru.mipt.acsl.decode.model.expr.ConstExpr
import ru.mipt.acsl.decode.model.naming.{ElementName, HasName}

/**
  * Created by metadeus on 08.03.16.
  */
trait EnumConstant extends Referenceable with HasName {

  def alias: Alias.EnumConstant

  def value: ConstExpr

  override def name: ElementName = alias.name

  def accept[T](visitor: ReferenceableVisitor[T]): T = {
    visitor.visit(this)
  }

}

object EnumConstant {

  private case class EnumConstantImpl(alias: Alias.EnumConstant, value: ConstExpr)
    extends EnumConstant

  def apply(alias: Alias.EnumConstant, value: ConstExpr): EnumConstant =
    EnumConstantImpl(alias, value)
}
