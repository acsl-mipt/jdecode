package ru.mipt.acsl.decode.model.domain

import java.util.Optional

/**
  * @author Artem Shein
  */
trait DecodeName {
  def asString(): String
}

object DecodeName {
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

trait DecodeUnit extends DecodeNameAware with DecodeOptionalInfoAware with DecodeReferenceable with DecodeNamespaceAware {
  def getDisplay: Optional[String]

  def accept[T] (visitor: DecodeReferenceableVisitor[T] ): T = visitor.visit(this)
}
