package ru.mipt.acsl.decode.generator.json

import java.io.{OutputStream, OutputStreamWriter}

import io.circe.Printer
import io.circe.generic.auto._
import io.circe.syntax._
import ru.mipt.acsl.common._
import ru.mipt.acsl.decode.model.component.message.{EventMessage, MessageParameter, MessageParameterPathElement, StatusMessage}
import ru.mipt.acsl.decode.model.component.{Command, Component}
import ru.mipt.acsl.decode.model.expr._
import ru.mipt.acsl.decode.model.naming.{HasName, Namespace}
import ru.mipt.acsl.decode.model.registry.{Language, Measure, Registry}
import ru.mipt.acsl.decode.model.types._
import ru.mipt.acsl.decode.model.{HasInfo, NamespaceAware}

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

    val units = MapBuffer[Measure, Json.Unit]()
    val types = MapBuffer[DecodeType, Json.Type]()
    val components = MapBuffer[Component, Json.Component]()
    val namespaces = MapBuffer[Namespace, Json.Namespace]()

    def generateRootComponents(cs: Seq[Component]): Json.Root = {
      cs.foreach(generate)
      Json.Root(units.buffer, types.buffer, components.buffer, namespaces.buffer)
    }

    private def generate(c: Component): Int =
      generate[Component, Json.Component](c, components,
        Json.Component(
        c.name.asMangledString,
        generate(c.namespace),
        c.subComponents.map { sc => Json.ComponentRef(sc.alias, generate(sc.component)) },
        c.baseType.map(generate(_)), c.commands.map(command), c.eventMessages.map(eventMessage),
          c.statusMessages.map(statusMessage)))

    private def constExpr(e: ConstExpr): Json.ConstExpr = e match {
      case f: BigDecimalLiteral => Json.NumberLiteral(f.value.toString)
      case i: BigIntLiteral => Json.NumberLiteral(i.value.toString)
      case _ => sys.error("not implemented")
    }

    private def name(named: HasName): String = named.name.asMangledString

    private def info(obj: HasInfo): Json.LocalizedString = localizedString(obj.info)

    private def statusMessage(s: StatusMessage): Json.StatusMessage =
      Json.StatusMessage(name(s), info(s), s.id, s.priority, s.parameters.map{ mp =>
        Json.MessageParameter(info(mp), mp.path.map(pathElement))})

    private def eventMessage(e: EventMessage): Json.EventMessage =
      Json.EventMessage(name(e), info(e), generate(e.baseType), e.id, e.fields.map(field))

    private def field(f: Either[MessageParameter, Parameter]): Json.Field = f match {
      case Left(mp) => Json.MessageParameter(info(mp), mp.path.map(pathElement))
      case Right(p) => Json.Parameter(name(p), info(p), typeUnit(p.typeUnit))
    }

    private def pathElement(p: MessageParameterPathElement): Json.ParameterPathElement = p match {
      case Left(el) => Json.ElementName(el.asMangledString)
      case Right(r) => Json.ArrayRange(r.min.toString, r.max.map(_.toString))
    }

    private def command(c: Command): Json.Command =
      Json.Command(name(c), info(c), c.parameters.map(parameter), generate(c.returnType))

    private def parameter(p: Parameter): Json.Parameter =
      Json.Parameter(name(p), info(p), typeUnit(p.typeUnit))

    private def localizedString(info: Map[Language, String]): Json.LocalizedString =
      Json.LocalizedString(info.map{ i => i._1.code -> i._2})

    private def typeUnit(tu: TypeMeasure): Json.TypeUnit =
      Json.TypeUnit(generate(tu.t), tu.unit.map(generate))

    private def structField(f: StructField): Json.StructField =
      Json.StructField(name(f), typeUnit(f.typeUnit))

    private def enumConst(c: EnumConstant): Json.EnumConst =
      Json.EnumConst(name(c), constExpr(c.value), info(c))

    private def generate[T, JT >: Null <: AnyRef](obj: T, mapBuffer: MapBuffer[T, JT], makeJObj: => JT): Int = {
      for (id <- mapBuffer.map.get(obj))
        return id
      val idx = mapBuffer.buffer.size
      mapBuffer.buffer += null
      mapBuffer.map(obj) = idx
      mapBuffer.buffer(idx) = makeJObj
      idx
    }

    private def generate(ns: Namespace): Int = {
      val idx = generate[Namespace, Json.Namespace](ns, namespaces,
        Json.Namespace(name(ns), info(ns), ns.parent.map(generate)))
      idx
    }

    private def appendToNamespace[T <: NamespaceAware](f: (Json.Namespace => mutable.Set[Int]), o: T, objIdx: Int): Int = {
      f(namespaces.buffer(generate(o.namespace))) += objIdx
      objIdx
    }

    private def generate(unit: Measure): Int =
      generate[Measure, Json.Unit](unit, units, Json.Unit(name(unit), info(unit), localizedString(unit.display)))

    private def generate(t: DecodeType): Int =
      generate[DecodeType, Json.Type](t, types, {
        val typeName = name(t)
        val typeInfo = info(t)
        t match {
          case a: AliasType => Json.Alias(typeName, typeInfo, generate(a.namespace), generate(a.baseType))
          case s: SubType => Json.SubType(typeName, typeInfo, generate(s.namespace), generate(s.baseType))
          case n: NativeType => Json.NativeType(typeName, typeInfo, generate(n.namespace))
          case s: StructType => Json.StructType(typeName, typeInfo, generate(s.namespace), s.fields.map(structField))
          case s: GenericTypeSpecialized => Json.GenericTypeSpecialized(typeName, typeInfo, generate(s.namespace),
            generate(s.genericType), s.genericTypeArguments.map(generate))
          case e: EnumType => new Json.EnumType(typeName, typeInfo, generate(e.namespace),
            e.extendsTypeOption.map(generate), e.baseTypeOption.map(generate), e.isFinal,
            e.constants.map(enumConst).toSeq)
          case _ => sys.error(s"not implemented for $t")
        }})
  }

}

case class DecodeJsonGeneratorConfig(registry: Registry, output: OutputStream, componentsFqn: Seq[String],
                                     prettyPrint: Boolean = false)
