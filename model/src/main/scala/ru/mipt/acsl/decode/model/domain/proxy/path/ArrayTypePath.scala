package ru.mipt.acsl.decode.model.domain.proxy.path

import ru.mipt.acsl.decode.model.domain.impl.naming.ElementName
import ru.mipt.acsl.decode.model.domain.naming.ElementName
import ru.mipt.acsl.decode.model.domain.types.ArraySize
import ru.mipt.acsl.decode.model.domain.impl.types._

/**
  * @author Artem Shein
  */
case class ArrayTypePath(baseTypePath: ProxyPath, arraySize: ArraySize) extends ProxyElementName {

  def mangledName: ElementName =
      ElementName.newFromMangledName("[" + baseTypePath.mangledName.asMangledString +
    (if (arraySize.isAny)
      ""
    else
      "," +
        (if (arraySize.isFixed)
          arraySize.min
        else
          arraySize.min + ".." +
            (if (arraySize.isLimited)
              arraySize.max
            else
              "*"))) + "]")

}
