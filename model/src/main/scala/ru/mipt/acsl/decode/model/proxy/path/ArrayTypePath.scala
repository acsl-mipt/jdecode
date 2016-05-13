package ru.mipt.acsl.decode.model.proxy.path

import ru.mipt.acsl.decode.model.naming.ElementName
import ru.mipt.acsl.decode.model.types.{ArraySize, _}

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
