package ru.mipt.acsl.decode.model.domain.impl.types

/**
  * @author Artem Shein
  */
private abstract class AbstractType(name: ElementName, var namespace: Namespace, info: Option[String])
  extends AbstractNameAndOptionInfoAware(name, info) with DecodeType {
  def fqn: Fqn = Fqn(namespace.fqn.parts :+ name)
}
