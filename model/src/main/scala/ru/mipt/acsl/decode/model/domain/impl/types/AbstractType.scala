package ru.mipt.acsl.decode.model.domain.impl.types

import ru.mipt.acsl.decode.model.domain.impl.naming.Namespace
import ru.mipt.acsl.decode.model.domain.pure.LocalizedString
import ru.mipt.acsl.decode.model.domain.pure.naming.ElementName

/**
  * @author Artem Shein
  */
abstract class AbstractType(name: ElementName, namespace: Namespace, info: LocalizedString)
  extends AbstractNameNamespaceInfoAware(name, namespace, info) with DecodeType
