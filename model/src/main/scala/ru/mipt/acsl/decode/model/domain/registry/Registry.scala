package ru.mipt.acsl.decode.model.domain.registry

import ru.mipt.acsl.decode.model.domain.{Referenceable, Resolvable, Validatable}
import ru.mipt.acsl.decode.model.domain.component.Component
import ru.mipt.acsl.decode.model.domain.component.messages.{EventMessage, StatusMessage}
import ru.mipt.acsl.decode.model.domain.naming.{HasName, Namespace}
import ru.mipt.acsl.decode.model.domain.proxy.ProxyPath
import ru.mipt.acsl.decode.model.domain.proxy.aliases._

import scala.collection.immutable
import scala.reflect.ClassTag

/**
  * Created by metadeus on 08.03.16.
  */
trait Registry extends Referenceable with HasName with Resolvable with Validatable {
  def rootNamespaces: immutable.Seq[Namespace]

  def rootNamespaces_=(rootNamespaces: immutable.Seq[Namespace])

  def resolve(): ResolvingResult

  def resolveElement[T <: Referenceable](path: ProxyPath)(implicit ct: ClassTag[T]): (Option[T], ResolvingResult)

  def allComponents: Seq[Component] = rootNamespaces.flatMap(_.allComponents)

  def allNamespaces: Seq[Namespace] = rootNamespaces.flatMap(_.allNamespaces)

  def component(fqn: String): Option[Component]

  def namespace(fqn: String): Option[Namespace]

  def eventMessage(fqn: String): Option[EventMessage]

  def statusMessage(fqn: String): Option[StatusMessage]

  def statusMessageOrFail(fqn: String): StatusMessage =
    statusMessage(fqn).getOrElse(sys.error("assertion error"))

  def eventMessageOrFail(fqn: String): EventMessage =
    eventMessage(fqn).getOrElse(sys.error("assertion error"))
}
