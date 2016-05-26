package ru.mipt.acsl.decode.generator.json

import io.circe
import io.circe.Encoder
import io.circe.generic.auto._
import io.circe.syntax._

/**
  * @author Artem Shein
  */
private object Json {

  case class LocalizedString(map: Map[String, String])

  sealed trait ConstExpr

  case class NumberLiteral(value: String) extends ConstExpr

  case class Unit(name: String, info: LocalizedString, display: LocalizedString)

  case class Root(units: Seq[Unit], types: Seq[Type], components: Seq[Component], namespaces: Seq[Namespace])

  sealed trait Type {
    def name: String

    def info: LocalizedString
  }

  case class Component(name: String, namespace: Int, subComponents: Seq[ComponentRef], baseType: Option[Int],
                       commands: Seq[Command], eventMessages: Seq[EventMessage], statusMessages: Seq[StatusMessage])

  case class ComponentRef(alias: Option[String], component: Int)

  case class Namespace(name: String, info: LocalizedString, parent: Option[Int] = None)

  case class Alias(name: String, info: LocalizedString, namespace: Int, baseType: Int, kind: String = "alias")
    extends Type

  case class SubType(name: String, info: LocalizedString, namespace: Int, baseType: Int, kind: String = "subtype") extends Type

  case class NativeType(name: String, info: LocalizedString, namespace: Int, kind: String = "native") extends Type

  case class StructField(name: String, typeUnit: TypeUnit)

  case class StructType(name: String, info: LocalizedString, namespace: Int, fields: Seq[StructField],
                        kind: String = "struct") extends Type

  case class GenericTypeSpecialized(name: String, info: LocalizedString, namespace: Int, genericType: Int,
                                    genericTypeArguments: Seq[Int], kind: String = "specialized") extends Type

  case class EnumType(name: String, info: LocalizedString, namespace: Int, extendsType: Option[Int],
                      baseType: Option[Int], isFinal: Boolean, constants: Seq[EnumConst],
                      kind: String = "enum") extends Type

  object Type

  case class EnumConst(name: String, value: ConstExpr, info: LocalizedString)

  case class TypeUnit(t: Int, unit: Option[Int])

  trait Field

  case class Parameter(name: String, info: LocalizedString, typeUnit: TypeUnit) extends Field

  trait ParameterPathElement

  case class ElementName(name: String) extends ParameterPathElement

  case class ArrayRange(min: String, max: Option[String]) extends ParameterPathElement

  case class MessageParameter(info: LocalizedString, path: Seq[ParameterPathElement]) extends Field

  case class Command(name: String, info: LocalizedString, parameters: Seq[Parameter], returnType: Int)

  case class EventMessage(name: String, info: LocalizedString, baseType: Int, id: Option[Int], fields: Seq[Field])

  case class StatusMessage(name: String, info: LocalizedString, id: Option[Int], priority: Option[Int],
                           parameters: Seq[MessageParameter])

  implicit val encoderLocalizedString: Encoder[LocalizedString] = Encoder.instance {
    case i if i.map.isEmpty => circe.Json.Null
    case i => i.map.asJson
  }

  implicit val encoderConstExpr: Encoder[ConstExpr] = Encoder.instance {
    case f: NumberLiteral => f.value.asJson
  }

  implicit val encodeType: Encoder[Type] = Encoder.instance {
    case a: Alias => a.asJson
    case s: SubType => s.asJson
    case s: StructType => s.asJson
    case e: EnumType => e.asJson
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
