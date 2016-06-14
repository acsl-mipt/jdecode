package ru.mipt.acsl.decode.model.types

/**
  * Created by metadeus on 04.06.16.
  */
trait ArrayTypeInfo {

  def baseType: DecodeType

}

object ArrayTypeInfo {

  private case class ArrayTypeInfoImpl(t: GenericTypeSpecialized) extends ArrayTypeInfo {

    def baseType: DecodeType = t.genericTypeArguments.get(0)

  }

  def apply(t: DecodeType): ArrayTypeInfo = t match {
    case a: GenericTypeSpecialized if a.isArray => ArrayTypeInfoImpl(a)
    case _ => throw new IllegalArgumentException("type must be an array")
  }


}
