package ru.mipt.acsl.decode.generator.json

import java.io.{OutputStream, OutputStreamWriter}

import io.circe.Printer
import io.circe.generic.auto._
import io.circe.syntax._
import ru.mipt.acsl.common._
import ru.mipt.acsl.decode.model.domain.{Language, NamespaceAware}
import ru.mipt.acsl.decode.model.domain.impl.component.Component
import ru.mipt.acsl.decode.model.domain.impl.naming.Namespace
import ru.mipt.acsl.decode.model.domain.impl.registry.{DecodeUnit, Registry}
import ru.mipt.acsl.decode.model.domain.impl.types.{AliasType, ArrayType, DecodeType, EnumType, GenericType, GenericTypeSpecialized, NativeType, StructField, StructType, SubType, TypeUnit}
import ru.mipt.acsl.decode.model.domain.pure.expr.{ConstExpr, FloatLiteral, IntLiteral}
import ru.mipt.acsl.decode.model.domain.pure.types.EnumConstant

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
      appendToNamespace(_.components, c, generate[Component, Json.Component](c, components, Json.Component(
        c.name.asMangledString,
        c.subComponents.map { sc => Json.ComponentRef(sc.alias, generate(sc.component)) },
        c.baseType.map(generate(_)))))

    private def constExpr(e: ConstExpr): Json.ConstExpr = e match {
      case f: FloatLiteral => Json.ConstExpr(f.value)
      case i: IntLiteral => Json.ConstExpr(i.value)
      case _ => sys.error("not implemented")
    }

    private def localizedString(info: Map[Language, String]): Json.LocalizedString = info.map{ i => i._1.code -> i._2}

    private def unit(u: DecodeUnit): Json.Unit =
      Json.Unit(u.name.asMangledString, localizedString(u.info), localizedString(u.display))

    private def typeUnit(tu: TypeUnit): Json.TypeUnit =
      Json.TypeUnit(generate(tu.t), tu.unit.map(generate(_)))

    private def structField(f: StructField): Json.StructField =
      Json.StructField(f.name.asMangledString, typeUnit(f.typeUnit))

    private def enumConst(c: EnumConstant): Json.EnumConst =
      Json.EnumConst(c.name.asMangledString, constExpr(c.value), localizedString(c.info))

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
        Json.Namespace(ns.name.asMangledString, localizedString(ns.info)))
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
        Json.Unit(unit.name.asMangledString, localizedString(unit.info), localizedString(unit.display))))

    private def generate(t: DecodeType): Int =
      appendToNamespace(_.types, t, generate[DecodeType, Json.Type](t, types, {
        val name = t.name.asMangledString
        val info = localizedString(t.info)
        t match {
          case a: AliasType => Json.Alias(name, info, generate(a.baseType))
          case s: SubType => Json.SubType(name, info, generate(s.baseType),
            s.range.map{ r => Json.SubTypeRange(r.from.map(constExpr(_)), r.to.map(constExpr(_))) })
          case n: NativeType => Json.NativeType(name, info)
          case a: ArrayType => Json.ArrayType(name, info, generate(a.baseType), a.size.min, a.size.max)
          case s: StructType => Json.StructType(name, info, s.fields.map(structField(_)))
          case g: GenericType => Json.GenericType(name, info, g.typeParameters.map(_.map(_.asMangledString)))
          case s: GenericTypeSpecialized => Json.GenericTypeSpecialized(name, info, generate(s.genericType),
            s.genericTypeArguments.map(_.map(generate(_))))
          case e: EnumType => new Json.EnumType(name, info, e.extendsTypeOption.map(generate(_)), e.baseTypeOption.map(generate(_)),
            e.isFinal, e.constants.map(enumConst(_)).toSeq)
          case _ => sys.error(s"not implemented for $t")
        }}))
  }

}

case class DecodeJsonGeneratorConfig(registry: Registry, output: OutputStream, rootComponentFqn: String,
                                     prettyPrint: Boolean = false)
