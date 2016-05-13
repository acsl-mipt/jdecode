package ru.mipt.acsl.decode.generator.json

import io.circe.Encoder
import io.circe.generic.auto._
import io.circe.syntax._

import scala.collection.mutable

/**
  * @author Artem Shein
  */
private object Json {

  type LocalizedString = Map[String, String]

  type ConstExpr = Either[Long, Float]

  object ConstExpr {
    def apply(value: Long): ConstExpr = Left(value)

    def apply(value: Float): ConstExpr = Right(value)
  }

  case class Unit(name: String, info: LocalizedString, display: LocalizedString)

  case class Root(units: Seq[Unit], types: Seq[Type], components: Seq[Component], namespaces: Seq[Namespace])

  sealed trait Type {
    def name: String

    def info: LocalizedString
  }

  case class Component(name: String, subComponents: Seq[ComponentRef], baseType: Option[Int], commands: Seq[Command],
                       eventMessages: Seq[EventMessage], statusMessages: Seq[StatusMessage])

  case class ComponentRef(alias: Option[String], component: Int)

  case class Namespace(name: String, info: LocalizedString, subNamespaces: mutable.Set[Int] = mutable.Set.empty,
                       units: mutable.Set[Int] = mutable.Set.empty,
                       types: mutable.Set[Int] = mutable.Set.empty,
                       components: mutable.Set[Int] = mutable.Set.empty)

  case class Alias(name: String, info: LocalizedString, baseType: Int, kind: String = "alias") extends Type

  case class SubType(name: String, info: LocalizedString, baseType: Int,
                     range: Option[SubTypeRange], kind: String = "subtype") extends Type

  case class NativeType(name: String, info: LocalizedString, kind: String = "native") extends Type

  case class ArrayType(name: String, info: LocalizedString, baseType: Int, min: Long, max: Long, kind: String = "array")
    extends Type

  case class StructField(name: String, typeUnit: TypeUnit)

  case class StructType(name: String, info: LocalizedString, fields: Seq[StructField], kind: String = "struct") extends Type

  case class GenericType(name: String, info: LocalizedString, typeParameters: Seq[Option[String]],
                         kind: String = "generic") extends Type

  case class GenericTypeSpecialized(name: String, info: LocalizedString, genericType: Int,
                                    genericTypeArguments: Seq[Option[Int]], kind: String = "specialized") extends Type

  case class EnumType(name: String, info: LocalizedString, extendsType: Option[Int], baseType: Option[Int],
                      isFinal: Boolean, constants: Seq[EnumConst], kind: String = "enum") extends Type

  object Type

  case class EnumConst(name: String, value: ConstExpr, info: LocalizedString)

  case class SubTypeRange(from: Option[ConstExpr], to: Option[ConstExpr])

  case class TypeUnit(t: Int, unit: Option[Int])

  trait Field

  case class Parameter(name: String, info: LocalizedString, typeUnit: TypeUnit) extends Field

  trait ParameterPathElement

  case class ElementName(name: String) extends ParameterPathElement

  case class ArrayRange(min: Long, max: Option[Long]) extends ParameterPathElement

  case class MessageParameter(info: LocalizedString, path: Seq[ParameterPathElement]) extends Field

  case class Command(name: String, info: LocalizedString, parameters: Seq[Parameter], returnType: Option[Int])

  case class EventMessage(name: String, info: LocalizedString, baseType: Int, id: Option[Int], fields: Seq[Field])

  case class StatusMessage(name: String, info: LocalizedString, id: Option[Int], priority: Option[Int],
                           parameters: Seq[MessageParameter])

  implicit val encodeType: Encoder[Type] = Encoder.instance {
    case a: Alias => a.asJson
    case s: SubType => s.asJson
    case a: ArrayType => a.asJson
    case s: StructType => s.asJson
    case e: EnumType => e.asJson
    case g: GenericType => g.asJson
    case s: GenericTypeSpecialized => s.asJson
    case n: NativeType => n.asJson
  }

  implicit val encodeField: Encoder[Field] = Encoder.instance {
    case p: Parameter => p.asJson
    case mp: MessageParameter => mp.asJson
  }

  implicit val encodePathElement: Encoder[ParameterPathElement] = Encoder.instance {
    case e: ElementName => e.asJson
    case ar: ArrayRange => ar.asJson
  }

}
