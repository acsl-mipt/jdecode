package ru.mipt.acsl.decode.model.domain.impl

import ru.mipt.acsl.decode.model.domain._
import ru.mipt.acsl.decode.model.domain.pure.naming.Fqn
import ru.mipt.acsl.decode.model.domain.pure.types.{ArraySize, EnumConstant}

package object types {

  import naming._

  implicit class DecodeTypeHelper(val t: DecodeType) {
    def fqn: Fqn = Fqn(t.namespace.fqn.parts :+ t.name)
  }

  implicit class NativeTypeJelper(val t: NativeType) {
    def isPrimitive: Boolean = PrimitiveTypeInfo.typeInfoByFqn.get(t.fqn).nonEmpty
    def primitiveTypeInfo: PrimitiveTypeInfo = PrimitiveTypeInfo.typeInfoByFqn(t.fqn)
  }

  implicit class EnumTypeHelper(val t: EnumType) {
    def allConstants: Set[EnumConstant] = t.constants ++ t.extendsType.map(_.allConstants).getOrElse(Set.empty)
  }

  implicit class ArrayTypeHelper(val t: ArrayType) {

    def isFixedSize: Boolean = t.size.isFixed

  }

  implicit class ArraySizeHelper(val s: pure.types.ArraySize) {

    def isFixed: Boolean = {
      val max = s.max
      s.min == max && max != 0
    }

    def isLimited: Boolean = s.max != 0

    def isAny: Boolean = s.min == 0 && s.max == 0

  }

}