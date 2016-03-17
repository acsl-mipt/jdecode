package ru.mipt.acsl.decode.model.domain.impl

import ru.mipt.acsl.decode.model.domain._
import ru.mipt.acsl.decode.model.domain.pure.naming.Fqn
import ru.mipt.acsl.decode.model.domain.pure.types.EnumConstant

package object types {

  import naming._

  implicit class DecodeTypeHelper(val t: DecodeType) {
    def fqn: Fqn = Fqn(t.namespace.fqn.parts :+ t.name)
  }

  implicit class EnumTypeHelper(val t: EnumType) {
    def allConstants: Set[EnumConstant] = t.constants ++ t.extendsType.map(_.allConstants).getOrElse(Set.empty)
  }

  implicit class ArrayTypeHelper(val t: ArrayType) {
    def isFixedSize: Boolean = {
      val thisSize: pure.types.ArraySize = t.size
      val maxLength: Long = thisSize.max
      thisSize.min == maxLength && maxLength != 0
    }
  }

}