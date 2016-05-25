package ru.mipt.acsl.decode.model.types

import ru.mipt.acsl.decode.model.LocalizedString
import ru.mipt.acsl.decode.model.naming.Namespace
import ru.mipt.acsl.decode.model.naming.ElementName
import ru.mipt.acsl.decode.model.proxy.MaybeProxy

/**
  * @author Artem Shein
  */
trait EnumType extends HasBaseType with GenericType {

  def isFinal: Boolean

  def constants: Set[EnumConstant]

  def extendsTypeOption: Option[EnumType] = extendsOrBaseType.left.toOption

  def baseTypeOption: Option[DecodeType] = extendsOrBaseType.right.toOption.map(_.t)

  def extendsOrBaseTypeProxy: Either[MaybeProxy[EnumType], TypeMeasure]

  def extendsOrBaseType: Either[EnumType, TypeMeasure] = extendsOrBaseTypeProxy.fold(l => Left(l.obj), r => Right(r))

  override def toString: String = "EnumType" + super.toString

}

object EnumType {

  private class Impl(name: ElementName, namespace: Namespace,
                     var extendsOrBaseTypeProxy: Either[MaybeProxy[EnumType], TypeMeasure],
                     info: LocalizedString, var constants: Set[EnumConstant], var isFinal: Boolean,
                     val typeParameters: Seq[ElementName])
    extends AbstractType(name, namespace, info) with EnumType {

    def extendsTypeProxy: Option[MaybeProxy[EnumType]] = extendsOrBaseTypeProxy.left.toOption

    override def extendsTypeOption: Option[EnumType] = extendsTypeProxy.map(_.obj)

    override def baseTypeProxy: MaybeProxy[DecodeType] =
      extendsOrBaseTypeProxy match {
        case Left(extendsType) => extendsType.asInstanceOf[MaybeProxy[DecodeType]] // fixme
        case Right(baseType) => baseType.typeProxy
      }
  }

  def apply(name: ElementName, namespace: Namespace,
            extendsOrBaseTypeProxy: Either[MaybeProxy[EnumType], TypeMeasure],
            info: LocalizedString, constants: Set[EnumConstant], isFinal: Boolean,
            typeParameters: Seq[ElementName]): EnumType =
    new Impl(name, namespace, extendsOrBaseTypeProxy, info, constants, isFinal, typeParameters)
}
