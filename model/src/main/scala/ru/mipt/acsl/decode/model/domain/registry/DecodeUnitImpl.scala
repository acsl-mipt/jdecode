package ru.mipt.acsl.decode.model.domain.registry

import ru.mipt.acsl.decode.model.domain.LocalizedString
import ru.mipt.acsl.decode.model.domain.impl.naming.Namespace
import ru.mipt.acsl.decode.model.domain.impl.types.AbstractNameNamespaceInfoAware
import ru.mipt.acsl.decode.model.domain.naming.ElementName

/**
  * @author Artem Shein
  */
private class DecodeUnitImpl(name: ElementName, namespace: Namespace, var display: LocalizedString,
                             info: LocalizedString)
  extends AbstractNameNamespaceInfoAware(name, namespace, info) with DecodeUnit
