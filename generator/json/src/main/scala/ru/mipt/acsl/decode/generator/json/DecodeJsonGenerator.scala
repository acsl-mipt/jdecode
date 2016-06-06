package ru.mipt.acsl.decode.generator.json

import java.io.{OutputStream, OutputStreamWriter}

import io.circe.Printer
import io.circe.generic.auto._
import io.circe.syntax._
import ru.mipt.acsl.common._
import ru.mipt.acsl.decode.model._
import ru.mipt.acsl.decode.model.component.message.{EventMessage, MessageParameterPathElement, StatusMessage}
import ru.mipt.acsl.decode.model.component.{Command, Component}
import ru.mipt.acsl.decode.model.expr._
import ru.mipt.acsl.decode.model.naming.{ElementName, HasName, Namespace}
import ru.mipt.acsl.decode.model.registry.{Language, Measure, Registry}
import ru.mipt.acsl.decode.model.types._

import scala.collection.mutable

case class DecodeJsonGenerator(config: DecodeJsonGeneratorConfig) {

  def generate(): Unit = {

    new OutputStreamWriter(config.output) {

      val rootComponents = config.componentsFqn.map(f => config.registry.component(f)
        .orElseFail(s"component not found $f"))

      private val jsonRoot = new StatefulDecodeJsonGenerator()
        .generateRootComponents(rootComponents)

      if (config.prettyPrint) {
        val printer = Printer.spaces2.copy(dropNullKeys = true)
        write(jsonRoot.asJson.pretty(printer))
      } else {
        val printer = Printer.noSpaces.copy(dropNullKeys = true)
        write(jsonRoot.asJson.pretty(printer))
      }

      flush()
    }
  }

  private class StatefulDecodeJsonGenerator {

    case class MapBuffer[T, JT](map: mutable.Map[T, Int] = mutable.Map[T, Int](), buffer: mutable.Buffer[JT] = mutable.Buffer[JT]())

    val objects = MapBuffer[Referenceable, Json.Referenceable]()

    def generateRootComponents(cs: Seq[Component]): Json.Root = {
      cs.foreach(generate)
      Json.Root(objects.buffer)
    }

    private def generate(c: Component): Int =
      generate(c,
        Json.Component(generate(c.alias), generate(c.namespace), c.baseType.map(generate), c.objects.map(generate)))

    private def constExpr(e: ConstExpr): Json.ConstExpr = e match {
      case f: BigDecimalLiteral => Json.NumberLiteral(f.value.toString)
      case i: BigIntLiteral => Json.NumberLiteral(i.value.toString)
      case _ => sys.error("not implemented")
    }

    private def name(name: ElementName): String = name.asMangledString

    private def name(named: HasName): String = named.name.asMangledString

    private def info(info: LocalizedString): Json.LocalizedString = localizedString(info)

    private def info(obj: HasInfo): Json.LocalizedString = localizedString(obj.info)

    private def pathElement(p: MessageParameterPathElement): Json.ParameterPathElement = p match {
      case Left(el) => Json.ElementName(el.asMangledString)
      case Right(r) => Json.ArrayRange(r.min.toString, r.max.map(_.toString))
    }

    private def localizedString(info: Map[Language, String]): Json.LocalizedString =
      Json.LocalizedString(info.map{ i => i._1.code -> i._2})

    private def typeMeasure(tm: TypeMeasure): Json.TypeMeasure =
      Json.TypeMeasure(generate(tm.t), tm.measure.map(generate))

    private def structField(f: StructField): Json.StructField =
      Json.StructField(generate(f.alias), typeMeasure(f.typeMeasure))

    private def enumConst(c: EnumConstant): Json.EnumConst =
      Json.EnumConst(generate(c.alias), constExpr(c.value))

    private def generate(obj: Referenceable, makeJObj: => Json.Referenceable): Int = {
      for (id <- objects.map.get(obj))
        return id
      val idx = objects.buffer.size
      objects.buffer += null
      objects.map(obj) = idx
      objects.buffer(idx) = makeJObj
      idx
    }

    private def generate(r: Referenceable): Int =
      generate(r, {
        r match {
          case ns: Namespace => Json.Namespace(generate(ns.alias), ns.parent.map(generate))
          case m: Measure => Json.Measure(generate(m.alias), localizedString(m.display))
          case s: SubType => Json.SubType(s.alias.map(generate), generate(s.namespace), generate(s.baseType))
          case n: NativeType => Json.NativeType(n.alias.map(generate).getOrElse(sys.error("must have an alias")), generate(n.namespace))
          case s: StructType => Json.StructType(s.alias.map(generate), generate(s.namespace), s.fields.map(structField))
          case s: GenericTypeSpecialized => Json.GenericTypeSpecialized(s.alias.map(generate), generate(s.namespace),
            generate(s.genericType), s.genericTypeArguments.map(generate))
          case e: EnumType => new Json.EnumType(e.alias.map(generate), generate(e.namespace),
            e.extendsTypeOption.map(generate), e.baseTypeOption.map(generate), e.isFinal,
            e.objects.map(generate))
          case c: Const => new Json.Const(c.alias.map(generate).getOrElse(sys.error("must have an alias")), generate(c.namespace), c.value)
          case tm: TypeMeasure => new Json.TypeMeasure(generate(tm.t), tm.measure.map(generate))
          case a: Alias => Json.Alias(name(a.name), info(a.info), generate(a.parent), 0)
          case ec: EnumConstant => Json.EnumConst(generate(ec.alias), constExpr(ec.value))
          case c: Command => Json.Command(generate(c.alias), c.objects.map(generate), generate(c.returnType))
          case com: CommandOrTmMessage => com.toEither match {
            case Left(c) => Json.Command(generate(c.alias), c.objects.map(generate), generate(c.returnType))
            case Right(tm) => tm match {
              case e: EventMessage => eventMessage(e)
              case s: StatusMessage => statusMessage(s)
            }
          }
          case e: EventMessage => eventMessage(e)
          case s: StatusMessage => statusMessage(s)
          case s: StatusParameter => Json.StatusParameter(info(s.info), s.path.map(pathElement))
          case p: Parameter => Json.Parameter(generate(p.alias), typeMeasure(p.typeMeasure))
          case _ => sys.error(s"not implemented for $r")
        }
      })

    def eventMessage(e: EventMessage): Json.EventMessage =
      Json.EventMessage(generate(e.alias), generate(e.baseType), e.id, e.objects.map(generate))

    def statusMessage(s: StatusMessage): Json.StatusMessage =
      Json.StatusMessage(generate(s.alias), s.id, s.priority, s.objects.map(generate))

  }

}

case class DecodeJsonGeneratorConfig(registry: Registry, output: OutputStream, componentsFqn: Seq[String],
                                     prettyPrint: Boolean = false)
