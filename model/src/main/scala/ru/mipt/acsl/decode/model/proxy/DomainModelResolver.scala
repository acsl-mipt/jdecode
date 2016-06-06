package ru.mipt.acsl.decode.model.proxy

import ru.mipt.acsl.decode.model.registry.Registry

/**
  * @author Artem Shein
  */
trait DomainModelResolver {
  def resolve(registry: Registry): ResolvingMessages
}
