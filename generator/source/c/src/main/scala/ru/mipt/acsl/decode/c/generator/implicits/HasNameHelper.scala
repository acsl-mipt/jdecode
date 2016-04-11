package ru.mipt.acsl.decode.c.generator.implicits

import ru.mipt.acsl.decode.model.domain.impl.component.Component
import ru.mipt.acsl.decode.model.domain.pure.naming.HasName
import ru.mipt.acsl.decode.c.generator.CSourceGenerator._

/**
  * @author Artem Shein
  */
private[generator] case class HasNameHelper(named: HasName) {

  def cName: String = named.name.asMangledString

  def fileName: String = named.name.asMangledString

  def cTypeName: String = named.name.asMangledString

  def mangledCName: String = {
    var methodName = cName
    if (keywords.contains(methodName))
      methodName = "_" + methodName
    methodName
  }

  def cStructFieldName(structComponent: Component, component: Component): String =
    ((if (structComponent == component) "" else component.cName) +
      named.mangledCName.capitalize).upperCamel2LowerCamel
}
