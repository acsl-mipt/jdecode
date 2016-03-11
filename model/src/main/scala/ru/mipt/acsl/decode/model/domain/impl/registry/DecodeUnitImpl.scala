package ru.mipt.acsl.decode.model.domain.impl.registry

import ru.mipt.acsl.decode.model.domain.aliases.{LocalizedString, ValidatingResult}
import ru.mipt.acsl.decode.model.domain.impl.types.AbstractNameAndOptionalInfoAware
import ru.mipt.acsl.decode.model.domain.naming.{ElementName, Namespace}
import ru.mipt.acsl.decode.model.domain.proxy.Result
import ru.mipt.acsl.decode.model.domain.registry.{DecodeUnit, Registry}

/**
  * @author Artem Shein
  */
private class DecodeUnitImpl(name: ElementName, var namespace: Namespace, var display: LocalizedString,
                             info: LocalizedString)
  extends AbstractNameAndOptionalInfoAware(name, info) with DecodeUnit {
  override def validate(registry: Registry): ValidatingResult = {
    // TODO
    Result.empty
  }
}
