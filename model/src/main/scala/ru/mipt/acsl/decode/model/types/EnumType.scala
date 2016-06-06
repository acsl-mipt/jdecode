package ru.mipt.acsl.decode.model.types

import ru.mipt.acsl.decode.model.Referenceable
import ru.mipt.acsl.decode.model.naming.{Container, ElementName, Namespace}
import ru.mipt.acsl.decode.model.proxy.MaybeProxy

/**
  * @author Artem Shein
  */
trait EnumType extends DecodeType with Container {

  def isFinal: Boolean

  def constants: Seq[EnumConstant] = objects.flatMap {
    case c: EnumConstant => Seq(c)
    case _ => Seq.empty
  }

  def extendsTypeOption: Option[EnumType] = extendsOrBaseTypeProxy.left.toOption.map(_.obj)

  def baseTypeOption: Option[DecodeType] = extendsOrBaseTypeProxy.right.toOption.map(_.t)

  def extendsOrBaseTypeProxy: Either[MaybeProxy.Enum, TypeMeasure]

  def extendsOrBaseType: DecodeType = extendsOrBaseTypeProxy.fold(l => l.obj, r => r.t)

  def eitherExtendsOrBaseType: Either[EnumType, TypeMeasure] = extendsOrBaseTypeProxy.fold(l => Left(l.obj), r => Right(r))

  def baseType: DecodeType = eitherExtendsOrBaseType.fold(l => l.baseType, r => r.t)

  def systemName: String = "EnumType@" + hashCode()

  override def toString: String =
    s"${this.getClass}{alias = $alias, namespace = $namespace, extendsOrBaseTypeProxy = $extendsOrBaseTypeProxy," +
      s" objects = $objects, isFinal = $isFinal, typeParameters = $typeParameters}"

}

object EnumType {

  private class EnumTypeImpl(val alias: Option[Alias.NsType], var namespace: Namespace,
                             var extendsOrBaseTypeProxy: Either[MaybeProxy.Enum, TypeMeasure],
                             var objects: Seq[Referenceable], var isFinal: Boolean,
                             val typeParameters: Seq[ElementName])
    extends EnumType {

    def extendsTypeProxy: Option[MaybeProxy.Enum] = extendsOrBaseTypeProxy.left.toOption

    override def extendsTypeOption: Option[EnumType] = extendsTypeProxy.map(_.obj)

    def baseTypeProxy: MaybeProxy.TypeProxy =
      extendsOrBaseTypeProxy match {
        case Left(extendsType) => extendsType
        case Right(baseType) => baseType.typeProxy
      }

  }

  def apply(alias: Option[Alias.NsType], namespace: Namespace,
            extendsOrBaseTypeProxy: Either[MaybeProxy.Enum, TypeMeasure],
            objects: Seq[Referenceable], isFinal: Boolean,
            typeParameters: Seq[ElementName]): EnumType =
    new EnumTypeImpl(alias, namespace, extendsOrBaseTypeProxy, objects, isFinal, typeParameters)
}
