package ru.mipt.acsl.decode.model.domain.impl.naming

import ru.mipt.acsl.decode.model.domain.aliases.{LocalizedString, ValidatingResult}
import ru.mipt.acsl.decode.model.domain.component.Component
import ru.mipt.acsl.decode.model.domain.naming.{ElementName, Fqn, HasName, Namespace}
import ru.mipt.acsl.decode.model.domain.proxy.aliases.ResolvingResult
import ru.mipt.acsl.decode.model.domain.registry.{DecodeUnit, Registry}
import ru.mipt.acsl.decode.model.domain.types.DecodeType

import scala.collection.{immutable, mutable}

/**
  * @author Artem Shein
  */
private class NamespaceImpl(var name: ElementName, var info: LocalizedString, var parent: Option[Namespace],
                            var types: immutable.Seq[DecodeType], var units: immutable.Seq[DecodeUnit],
                            var subNamespaces: immutable.Seq[Namespace], var components: immutable.Seq[Component])
  extends HasName with Namespace {
  override def asString: String = name.asMangledString

  override def resolve(registry: Registry): ResolvingResult = {
    val result = new mutable.ArrayBuffer[ResolvingResult]()
    result ++= types.map(_.resolve(registry))
    result ++= subNamespaces.map(_.resolve(registry))
    result ++= components.map(_.resolve(registry))
    result.flatten
  }

  override def validate(registry: Registry): ValidatingResult = {
    val result = new mutable.ArrayBuffer[ValidatingResult]()
    result ++= types.map(_.validate(registry))
    result ++= subNamespaces.map(_.validate(registry))
    result ++= components.map(_.validate(registry))
    result.flatten
  }

  override def fqn: Fqn = {
    val parts: scala.collection.mutable.Buffer[ElementName] = scala.collection.mutable.Buffer[ElementName]()
    var currentNamespace: Namespace = this
    while (currentNamespace.parent.isDefined) {
      parts += currentNamespace.name
      currentNamespace = currentNamespace.parent.get
    }
    parts += currentNamespace.name
    Fqn(parts.reverse)
  }
}
