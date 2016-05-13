package ru.mipt.acsl.decode.model.component.message

import ru.mipt.acsl.decode.model.{LocalizedString, _}
import ru.mipt.acsl.decode.model.component.message

trait MessageParameter extends HasInfo {
  def path: MessageParameterPath
}

object MessageParameter {

  private class Impl(val path: MessageParameterPath, val info: LocalizedString = LocalizedString.empty)
    extends MessageParameter {

    override def toString: String = path.map(_.fold(_.asMangledString.mkString(".", "", ""), _.toString.mkString("[", "", "]"))).mkString.substring(1)

  }

  def apply(path: MessageParameterPath, info: LocalizedString = LocalizedString.empty): MessageParameter =
    new Impl(path, info)
}