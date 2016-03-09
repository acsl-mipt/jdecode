package ru.mipt.acsl.decode.model.domain.impl.types

/**
  * @author Artem Shein
  */
object EnumConstant {
  def apply(name: ElementName, value: ConstExpr, info: Option[String]): EnumConstant =
    new EnumConstantImpl(name, value, info)
}
