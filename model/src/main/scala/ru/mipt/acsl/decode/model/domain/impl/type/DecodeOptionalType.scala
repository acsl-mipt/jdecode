package ru.mipt.acsl.decode.model.domain.impl.`type`

import java.util
import java.util.Optional

import ru.mipt.acsl.decode.model.domain.`type`.DecodeGenericType
import ru.mipt.acsl.decode.model.domain.impl.{DecodeNameImpl, DecodeNameImpl$}
import ru.mipt.acsl.decode.model.domain.impl.`type`.AbstractDecodeType
import ru.mipt.acsl.decode.model.domain.{DecodeName, DecodeName$, DecodeNamespace}

import scala.collection.JavaConverters._

/**
  * @author Artem Shein
  */

class DecodeOptionalType(name: Optional[DecodeName], ns: DecodeNamespace, info: Optional[String])
  extends AbstractDecodeType(name, ns, info) with DecodeGenericType {
  override def getTypeParameters: util.List[Optional[DecodeName]] = DecodeOptionalType.typeParameters
}

object DecodeOptionalType {
  private val typeParameters: util.List[Optional[DecodeName]] = Seq[Optional[DecodeName]](Optional.of(DecodeNameImpl.newFromMangledName("T"))).asJava
  val NAME: String = "optional"
  val MANGLED_NAME: DecodeNameImpl = DecodeNameImpl.newFromMangledName(NAME)
}
