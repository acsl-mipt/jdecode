package ru.mipt.acsl.decode.model.domain

import ru.mipt.acsl.decode.model.domain.naming.{HasName, Namespace}
import ru.mipt.acsl.decode.model.domain.proxy.aliases.ResolvingResult
import ru.mipt.acsl.decode.model.domain.registry.{Language, Registry}

import scala.collection.immutable

import ru.mipt.acsl.decode.model.domain.aliases._

trait Resolvable {
  def resolve(registry: Registry): ResolvingResult
}

trait Validatable {
  def validate(registry: Registry): ValidatingResult
}

trait HasInfo {
  def info: immutable.Map[Language, String]
}

trait HasOptionId {
  def id: Option[Int]
}

trait Referenceable extends HasName

trait NamespaceAware {
  def namespace: Namespace

  def namespace_=(namespace: Namespace)
}

trait HasNameAndInfo extends HasInfo with HasName

trait DomainModelResolver {
  def resolve(registry: Registry): ResolvingResult
}


