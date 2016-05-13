package ru.mipt.acsl.decode.model.types

import ru.mipt.acsl.decode.model.{LocalizedString, NamespaceAware}
import ru.mipt.acsl.decode.model.naming.Namespace
import ru.mipt.acsl.decode.model.naming.ElementName

/**
  * @author Artem Shein
  */
private[model] class AbstractNameNamespaceInfoAware(name: ElementName, var namespace: Namespace,
                                                     info: LocalizedString)
  extends AbstractNameInfoAware(name, info) with NamespaceAware
