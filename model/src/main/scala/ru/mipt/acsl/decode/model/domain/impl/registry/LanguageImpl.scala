package ru.mipt.acsl.decode.model.domain.impl.registry

/**
  * @author Artem Shein
  */
private class LanguageImpl(name: ElementName, namespace: Namespace, val isDefault: Boolean, info: Option[String])
  extends AbstractNameNamespaceOptionalInfoAware(name, namespace, info) with Language
