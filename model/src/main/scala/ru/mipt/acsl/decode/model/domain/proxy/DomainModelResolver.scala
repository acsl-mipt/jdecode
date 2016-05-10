package ru.mipt.acsl.decode.model.domain.proxy

import ru.mipt.acsl.decode.model.domain.registry.Registry

/**
  * @author Artem Shein
  */
trait DomainModelResolver {
  def resolve(registry: Registry): ResolvingResult
}
