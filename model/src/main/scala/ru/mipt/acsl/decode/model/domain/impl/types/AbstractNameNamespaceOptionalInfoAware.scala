package ru.mipt.acsl.decode.model.domain.impl.types

import ru.mipt.acsl.decode.model.domain.NamespaceAware
import ru.mipt.acsl.decode.model.domain.aliases.LocalizedString
import ru.mipt.acsl.decode.model.domain.naming.{ElementName, Namespace}

/**
  * @author Artem Shein
  */
private[domain] class AbstractNameNamespaceOptionalInfoAware(name: ElementName, var namespace: Namespace,
                                                             info: LocalizedString)
  extends AbstractNameAndOptionalInfoAware(name, info) with NamespaceAware
