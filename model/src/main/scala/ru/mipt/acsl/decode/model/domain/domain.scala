package ru.mipt.acsl.decode.model.domain

import ru.mipt.acsl.decode.model.domain.component.Component
import ru.mipt.acsl.decode.model.domain.component.messages.{EventMessage, StatusMessage}
import ru.mipt.acsl.decode.model.domain.naming.{ElementName, Fqn, Namespace}
import ru.mipt.acsl.decode.model.domain.proxy.aliases.ResolvingResult
import ru.mipt.acsl.decode.model.domain.proxy.{MaybeProxy, ProxyPath}
import ru.mipt.acsl.decode.model.domain.types._

import scala.collection.immutable
import scala.reflect.ClassTag

package object aliases {
  type MessageParameterToken = Either[String, Int]
  type ValidatingResult = ResolvingResult
}

import ru.mipt.acsl.decode.model.domain.aliases._

trait Resolvable {
  def resolve(registry: Registry): ResolvingResult
}

trait Validatable {
  def validate(registry: Registry): ValidatingResult
}

trait HasOptionName {
  def optionName: Option[ElementName]
}

trait HasName {
  def name: ElementName
}

trait HasOptionInfo {
  def info: Option[String]
}

trait HasOptionId {
  def id: Option[Int]
}

trait Referenceable extends HasName

trait Language extends Referenceable with NamespaceAware

trait NamespaceAware {
  def namespace: Namespace

  def namespace_=(namespace: Namespace)
}

trait NameAndOptionInfoAware extends HasOptionInfo with HasName

trait DecodeUnit extends HasName with HasOptionInfo with Referenceable with NamespaceAware with Validatable {
  def display: Option[String]
}

trait HasBaseType {
  def baseType: MaybeProxy[DecodeType]
}

trait EnumConstant extends HasOptionInfo {
  def name: ElementName

  def value: String
}

trait Fqned extends HasName with NamespaceAware {
  def fqn: Fqn
}

trait DomainModelResolver {
  def resolve(registry: Registry): ResolvingResult
}

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
