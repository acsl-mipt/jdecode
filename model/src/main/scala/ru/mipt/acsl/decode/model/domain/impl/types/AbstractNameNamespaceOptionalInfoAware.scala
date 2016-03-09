package ru.mipt.acsl.decode.model.domain.impl.types

/**
  * @author Artem Shein
  */
private[domain] class AbstractNameNamespaceOptionalInfoAware(name: ElementName, var namespace: Namespace, info: Option[String])
  extends AbstractNameAndOptionalInfoAware(name, info) with NamespaceAware
