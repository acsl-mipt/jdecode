package ru.mipt.acsl.decode.model.naming

/**
  * @author Artem Shein
  */
trait ElementName {
  def asMangledString: String
}

object ElementName {

  private case class Impl(value: String) extends ElementName {

    override def asMangledString: String = value

  }

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

  def newFromSourceName(name: String): ElementName = Impl(ElementName.mangleName(name))

  def newFromMangledName(name: String): ElementName = Impl(name)
}
