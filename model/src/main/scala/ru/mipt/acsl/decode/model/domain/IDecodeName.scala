package ru.mipt.acsl.decode.model.domain

/**
  * @author Artem Shein
  */
trait IDecodeName {
  def asString(): String
}

object IDecodeName {
  def mangleName(name: String): String = {
    var result = name
    if (result.startsWith("^")) {
      result = result.substring(1)
    }
    result = "[ \\\\^]".r.replaceAllIn(result, "")
    if (result.isEmpty)
      sys.error("invalid name")
    result
  }
}
