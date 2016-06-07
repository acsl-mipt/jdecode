package ru.mipt.acsl.decode.model

import scala.collection.JavaConversions._

package object types {

  implicit class NativeTypeHelper(val t: NativeType) {
    def isPrimitive: Boolean = sys.error("not implemented")//PrimitiveTypeInfo.typeInfoByFqn.get(t.fqn).nonEmpty
    def primitiveTypeInfo: PrimitiveTypeInfo = sys.error("not implemented")//PrimitiveTypeInfo.typeInfoByFqn(t.fqn)
  }

  implicit class ArraySizeHelper(val s: ArraySize) {

    def isFixed: Boolean = {
      val max = s.max
      s.min == max && max != 0
    }

    def isLimited: Boolean = s.max != 0

    def isAny: Boolean = s.min == 0 && s.max == 0

  }

}