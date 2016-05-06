package ru.mipt.acsl.ppf

/**
  * @author Artem Shein
  */
package object types {

  trait BaseType

  object Varuint extends BaseType {
    override def toString: String = "varuint"
  }

  case class FixedBaseType(name: String) extends BaseType {
    override def toString: String = name
  }

  object U8 extends FixedBaseType("u8")

  object U16 extends FixedBaseType("u16")

  object U32 extends FixedBaseType("u32")

}
