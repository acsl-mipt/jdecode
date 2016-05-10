package ru.mipt.acsl.decode.model.domain.impl.types

import ru.mipt.acsl.decode.model.domain.{LocalizedString, NamespaceAware}
import ru.mipt.acsl.decode.model.domain.impl.naming.Namespace
import ru.mipt.acsl.decode.model.domain.naming.ElementName

/**
  * @author Artem Shein
  */
private[domain] class AbstractNameNamespaceInfoAware(name: ElementName, var namespace: Namespace,
                                                     info: LocalizedString)
  extends AbstractNameInfoAware(name, info) with NamespaceAware
