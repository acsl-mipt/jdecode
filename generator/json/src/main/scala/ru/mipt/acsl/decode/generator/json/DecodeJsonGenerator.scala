package ru.mipt.acsl.decode.generator.json

import java.io.{OutputStream, OutputStreamWriter}

import io.circe.Printer
import io.circe.generic.auto._
import io.circe.syntax._
import ru.mipt.acsl.common._
import ru.mipt.acsl.decode.model.component.{Command, Component}
import ru.mipt.acsl.decode.model.expr.{ConstExpr, FloatLiteral, IntLiteral}
import ru.mipt.acsl.decode.model.naming.{HasName, Namespace}
import ru.mipt.acsl.decode.model.registry.{DecodeUnit, Language, Registry}
import ru.mipt.acsl.decode.model.types.{EnumType, EnumConstant => _, _}
import ru.mipt.acsl.decode.model.types._
import ru.mipt.acsl.decode.model.{HasInfo, NamespaceAware}
import ru.mipt.acsl.decode.model.component.message.{EventMessage, MessageParameter, MessageParameterPath, MessageParameterPathElement, StatusMessage}

import scala.collection.mutable

class DecodeJsonGenerator(val config: DecodeJsonGeneratorConfig) {

  def generate(): Unit = {

    new OutputStreamWriter(config.output) {

      val rootComponent = config.registry.component(config.rootComponentFqn)
        .orElseFail("component not found")

      private val jsonRoot = new StatefulDecodeJsonGenerator()
        .generateRootComponent(rootComponent)

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

    val units = MapBuffer[DecodeUnit, Json.Unit]()
    val types = MapBuffer[DecodeType, Json.Type]()
    val components = MapBuffer[Component, Json.Component]()
    val namespaces = MapBuffer[Namespace, Json.Namespace]()

    def generateRootComponent(c: Component): Json.Root = {
      generate(c)
      Json.Root(units.buffer, types.buffer, components.buffer, namespaces.buffer)
    }

    private def generate(c: Component): Int =
      appendToNamespace(_.components, c, generate[Component, Json.Component](c, components,
        Json.Component(
        c.name.asMangledString,
        c.subComponents.map { sc => Json.ComponentRef(sc.alias, generate(sc.component)) },
        c.baseType.map(generate(_)), c.commands.map(command), c.eventMessages.map(eventMessage),
          c.statusMessages.map(statusMessage))))

    private def constExpr(e: ConstExpr): Json.ConstExpr = e match {
      case f: FloatLiteral => Json.ConstExpr(f.value)
      case i: IntLiteral => Json.ConstExpr(i.value)
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
      case Right(r) => Json.ArrayRange(r.min, r.max)
    }

    private def command(c: Command): Json.Command =
      Json.Command(name(c), info(c), c.parameters.map(parameter), c.returnType.map(generate))

    private def parameter(p: Parameter): Json.Parameter =
      Json.Parameter(name(p), info(p), typeUnit(p.typeUnit))

    private def localizedString(info: Map[Language, String]): Json.LocalizedString = info.map{ i => i._1.code -> i._2}

    private def unit(u: DecodeUnit): Json.Unit =
      Json.Unit(name(u), info(u), localizedString(u.display))

    private def typeUnit(tu: TypeUnit): Json.TypeUnit =
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
        Json.Namespace(name(ns), info(ns)))
      for (parentNs <- ns.parent)
        namespaces.buffer(generate(parentNs)).subNamespaces += idx
      idx
    }

    private def appendToNamespace[T <: NamespaceAware](f: (Json.Namespace => mutable.Set[Int]), o: T, objIdx: Int): Int = {
      f(namespaces.buffer(generate(o.namespace))) += objIdx
      objIdx
    }

    private def generate(unit: DecodeUnit): Int =
      appendToNamespace(_.units, unit, generate[DecodeUnit, Json.Unit](unit, units,
        Json.Unit(name(unit), info(unit), localizedString(unit.display))))

    private def generate(t: DecodeType): Int =
      appendToNamespace(_.types, t, generate[DecodeType, Json.Type](t, types, {
        val typeName = name(t)
        val typeInfo = info(t)
        t match {
          case a: AliasType => Json.Alias(typeName, typeInfo, generate(a.baseType))
          case s: SubType => Json.SubType(typeName, typeInfo, generate(s.baseType),
            s.range.map{ r => Json.SubTypeRange(r.from.map(constExpr), r.to.map(constExpr)) })
          case n: NativeType => Json.NativeType(typeName, typeInfo)
          case a: ArrayType => Json.ArrayType(typeName, typeInfo, generate(a.baseType), a.size.min, a.size.max)
          case s: StructType => Json.StructType(typeName, typeInfo, s.fields.map(structField))
          case g: GenericType => Json.GenericType(typeName, typeInfo, g.typeParameters.map(_.map(_.asMangledString)))
          case s: GenericTypeSpecialized => Json.GenericTypeSpecialized(typeName, typeInfo, generate(s.genericType),
            s.genericTypeArguments.map(_.map(generate)))
          case e: EnumType => new Json.EnumType(typeName, typeInfo, e.extendsTypeOption.map(generate),
            e.baseTypeOption.map(generate),
            e.isFinal, e.constants.map(enumConst).toSeq)
          case _ => sys.error(s"not implemented for $t")
        }}))
  }

}

case class DecodeJsonGeneratorConfig(registry: Registry, output: OutputStream, rootComponentFqn: String,
                                     prettyPrint: Boolean = false)
