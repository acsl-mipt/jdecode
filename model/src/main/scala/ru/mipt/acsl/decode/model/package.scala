package ru.mipt.acsl.decode

import java.util
import ru.mipt.acsl.decode.model.component.Component
import ru.mipt.acsl.decode.model.component.message._
import ru.mipt.acsl.decode.model.naming.{ElementName, HasName}
import ru.mipt.acsl.decode.model.proxy.{MaybeProxy, ResolvingMessages}
import ru.mipt.acsl.decode.model.registry.{Language, Measure}
import ru.mipt.acsl.decode.model.types.{Alias, DecodeType, TypeMeasure}

import scala.collection.immutable

package object model {

  type ValidatingResult = ResolvingMessages

  trait StatusParameter extends TmParameter {

    def path: MessageParameterPath

    def ref(component: Component): MessageParameterRef =
      new MessageParameterRefWalker(component, None, path)

  }

  object StatusParameter {

    private final class StatusParameterImpl(val path: MessageParameterPath, val info: util.Map[Language, String] = util.Collections.emptyMap())
      extends StatusParameter {

      override def toString: String = path.map(_.fold(_.mangledNameString.mkString(".", "", ""), _.toString.mkString("[", "", "]"))).mkString.substring(1)

    }

    def apply(path: MessageParameterPath, info: util.Map[Language, String] = util.Collections.emptyMap()): StatusParameter =
      new StatusParameterImpl(path, info)

    def unapply(p: StatusParameter): Option[(MessageParameterPath, util.Map[Language, String])] =
      Some((p.path, p.info))
  }

}