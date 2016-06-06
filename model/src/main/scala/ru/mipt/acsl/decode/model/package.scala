package ru.mipt.acsl.decode

import ru.mipt.acsl.decode.model.component.Component
import ru.mipt.acsl.decode.model.component.message._
import ru.mipt.acsl.decode.model.naming.{ElementName, HasName}
import ru.mipt.acsl.decode.model.proxy.{MaybeProxy, ResolvingMessages}
import ru.mipt.acsl.decode.model.registry.{Language, Measure}
import ru.mipt.acsl.decode.model.types.{Alias, DecodeType, TypeMeasure}

import scala.collection.immutable

package object model {

  type ValidatingResult = ResolvingMessages
  type LocalizedString = immutable.Map[Language, String]

  sealed trait TmParameter extends HasInfo with Referenceable

  trait StatusParameter extends TmParameter {

    def path: MessageParameterPath

    def ref(component: Component): MessageParameterRef =
      new MessageParameterRefWalker(component, None, path)

  }

  object StatusParameter {

    private final class StatusParameterImpl(val path: MessageParameterPath, val info: LocalizedString = LocalizedString.empty)
      extends StatusParameter {

      override def toString: String = path.map(_.fold(_.asMangledString.mkString(".", "", ""), _.toString.mkString("[", "", "]"))).mkString.substring(1)

    }

    def apply(path: MessageParameterPath, info: LocalizedString = LocalizedString.empty): StatusParameter =
      new StatusParameterImpl(path, info)

    def unapply(p: StatusParameter): Option[(MessageParameterPath, LocalizedString)] =
      Some((p.path, p.info))
  }

  trait Parameter extends TmParameter with HasName {

    def alias: Alias.MessageOrCommandParameter

    def typeMeasure: TypeMeasure

    def typeProxy: MaybeProxy.TypeProxy = typeMeasure.typeProxy

    def parameterType: DecodeType = typeProxy.obj

    def measureProxy: Option[MaybeProxy.Measure] = typeMeasure.measureProxy

    def measure: Option[Measure] = measureProxy.map(_.obj)

    override def name: ElementName = alias.name

    override def info: LocalizedString = alias.info

  }

  object Parameter {

    private final case class ParameterImpl(alias: Alias.MessageOrCommandParameter, typeMeasure: TypeMeasure)
      extends Parameter

    def apply(alias: Alias.MessageOrCommandParameter, typeUnit: TypeMeasure): Parameter =
      ParameterImpl(alias, typeUnit)

    def unapply(p: Parameter): Option[(Alias.MessageOrCommandParameter, TypeMeasure)] =
      Some((p.alias, p.typeMeasure))

  }

}