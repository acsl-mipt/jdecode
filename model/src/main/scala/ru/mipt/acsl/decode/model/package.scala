package ru.mipt.acsl.decode

import java.util
import scala.collection.JavaConversions._
import ru.mipt.acsl.decode.model.component.{Component, MessageParameterPath, StatusParameter}
import ru.mipt.acsl.decode.model.component.message._
import ru.mipt.acsl.decode.model.registry.Language

package object model {

  object StatusParameter {

    private final class StatusParameterImpl(val path: MessageParameterPath, val info: util.Map[Language, String] = util.Collections.emptyMap())
      extends StatusParameter {

      override def toString: String = path.elements().map{ el =>
        if (el.isElementName)
          el.elementName().get().mangledNameString.mkString(".", "", "")
        else
          el.arrayRange().get().toString.mkString("[", "", "]")
      }.mkString.substring(1)

    }

    def apply(path: MessageParameterPath, info: util.Map[Language, String] = util.Collections.emptyMap()): StatusParameter =
      new StatusParameterImpl(path, info)

    def unapply(p: StatusParameter): Option[(MessageParameterPath, util.Map[Language, String])] =
      Some((p.path, p.info))
  }

}