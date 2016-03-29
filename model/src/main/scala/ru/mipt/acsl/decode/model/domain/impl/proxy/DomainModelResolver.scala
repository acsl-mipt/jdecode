package ru.mipt.acsl.decode.model.domain.impl.proxy

import ru.mipt.acsl.decode.model.domain.pure.registry.Registry

/**
  * @author Artem Shein
  */
trait DomainModelResolver {
  def resolve(registry: Registry): ResolvingResult
}
