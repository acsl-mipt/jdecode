package ru.mipt.acsl.decode.model.domain.impl.registry

import ru.mipt.acsl.decode.model.domain.impl.naming.Namespace
import ru.mipt.acsl.decode.model.domain.impl.types.AbstractNameNamespaceInfoAware
import ru.mipt.acsl.decode.model.domain.pure.LocalizedString
import ru.mipt.acsl.decode.model.domain.pure.naming.ElementName

/**
  * @author Artem Shein
  */
private class DecodeUnitImpl(name: ElementName, namespace: Namespace, var display: LocalizedString,
                             info: LocalizedString)
  extends AbstractNameNamespaceInfoAware(name, namespace, info) with DecodeUnit
