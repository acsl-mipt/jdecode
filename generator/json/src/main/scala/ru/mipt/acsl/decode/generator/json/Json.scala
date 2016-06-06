package ru.mipt.acsl.decode.generator.json

import io.circe
import io.circe.Encoder
import io.circe.generic.auto._
import io.circe.syntax._

/**
  * @author Artem Shein
  */
private object Json {

  sealed trait Referenceable

  case class LocalizedString(map: Map[String, String])

  sealed trait ConstExpr

  case class NumberLiteral(value: String) extends ConstExpr

  case class Measure(alias: Int, display: LocalizedString)
    extends Referenceable

  case class Root(objects: Seq[Referenceable])

  sealed trait Type extends Referenceable

  case class Component(alias: Int, namespace: Int, baseType: Option[Int],
                       objects: Seq[Int], kind: String = "component")
    extends Referenceable

  case class ComponentRef(alias: String, component: Int)

  case class Namespace(alias: Int, parent: Option[Int] = None, kind: String = "namespace")
    extends Referenceable

  case class Alias(name: String, info: LocalizedString, namespace: Int, obj: Int, kind: String = "alias")
    extends Type

  case class SubType(alias: Option[Int], namespace: Int, baseType: Int, kind: String = "subtype") extends Type

  case class NativeType(alias: Int, namespace: Int, kind: String = "native") extends Type

  case class StructField(alias: Int, typeUnit: TypeMeasure) extends Referenceable

  case class StructType(alias: Option[Int], namespace: Int, objects: Seq[Referenceable],
                        kind: String = "struct") extends Type

  case class GenericTypeSpecialized(alias: Option[Int], namespace: Int, genericType: Int,
                                    genericTypeArguments: Seq[Int], kind: String = "specialized") extends Type

  case class EnumType(alias: Option[Int], namespace: Int, extendsType: Option[Int],
                      baseType: Option[Int], isFinal: Boolean, objects: Seq[Int],
                      kind: String = "enum") extends Type

  case class Const(alias: Int, namespace: Int, value: String) extends Type

  object Type

  case class EnumConst(alias: Int, value: ConstExpr)
    extends Referenceable

  case class TypeMeasure(t: Int, unit: Option[Int]) extends Type

  trait Field extends Referenceable

  case class Parameter(alias: Int, typeMeasure: TypeMeasure) extends Field

  trait ParameterPathElement

  case class ElementName(name: String) extends ParameterPathElement

  case class ArrayRange(min: String, max: Option[String]) extends ParameterPathElement

  case class StatusParameter(info: LocalizedString, path: Seq[ParameterPathElement]) extends Field

  case class Command(alias: Int, objects: Seq[Int], returnType: Int)
    extends Referenceable

  case class EventMessage(alias: Int, baseType: Int, id: Option[Int], objects: Seq[Int])
    extends Referenceable

  case class StatusMessage(alias: Int, id: Option[Int], priority: Option[Int],
                           objects: Seq[Int])
    extends Referenceable

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
    case c: Const => c.asJson
    case tm: TypeMeasure => tm.asJson
  }

  implicit val encodeField: Encoder[Field] = Encoder.instance {
    case p: Parameter => p.asJson
    case mp: StatusParameter => mp.asJson
  }

  implicit val encodePathElement: Encoder[ParameterPathElement] = Encoder.instance {
    case e: ElementName => e.asJson
    case ar: ArrayRange => ar.asJson
  }

  implicit val encodeReferenceable: Encoder[Referenceable] = Encoder.instance {
    case t: Type => t.asJson
    case c: Component => c.asJson
    case m: Measure => m.asJson
    case ns: Namespace => ns.asJson
    case sf: StructField => sf.asJson
    case e: EnumConst => e.asJson
    case c: Command => c.asJson
    case p: Parameter => p.asJson
    case sm: StatusMessage => sm.asJson
    case sp: StatusParameter => sp.asJson
  }

}
