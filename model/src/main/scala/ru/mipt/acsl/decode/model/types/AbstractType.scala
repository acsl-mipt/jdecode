package ru.mipt.acsl.decode.model.types

import ru.mipt.acsl.decode.model.LocalizedString
import ru.mipt.acsl.decode.model.naming.Namespace
import ru.mipt.acsl.decode.model.naming.ElementName
import ru.mipt.acsl.decode.model.types.DecodeType

/**
  * @author Artem Shein
  */
abstract class AbstractType(name: ElementName, namespace: Namespace, info: LocalizedString)
  extends AbstractNameNamespaceInfoAware(name, namespace, info) with DecodeType
