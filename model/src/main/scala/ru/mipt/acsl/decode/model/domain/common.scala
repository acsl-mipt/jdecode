package ru.mipt.acsl.decode.model.domain

import ru.mipt.acsl.decode.model.domain.naming.{HasName, Namespace}
import ru.mipt.acsl.decode.model.domain.proxy.aliases.ResolvingResult
import ru.mipt.acsl.decode.model.domain.registry.Registry

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

trait HasOptionInfo {
  def info: Option[String]
}

trait HasOptionId {
  def id: Option[Int]
}

trait Referenceable extends HasName

trait NamespaceAware {
  def namespace: Namespace

  def namespace_=(namespace: Namespace)
}

trait NameAndOptionInfoAware extends HasOptionInfo with HasName

trait DomainModelResolver {
  def resolve(registry: Registry): ResolvingResult
}


