package ru.mipt.acsl.decode.model.domain.impl.`type`

import java.util
import java.util.Optional

import ru.mipt.acsl.decode.model.domain.`type`.DecodeGenericType
import ru.mipt.acsl.decode.model.domain.impl.DecodeName
import ru.mipt.acsl.decode.model.domain.impl.`type`.AbstractDecodeType
import ru.mipt.acsl.decode.model.domain.{IDecodeName, DecodeNamespace}

import scala.collection.JavaConverters._

/**
  * @author Artem Shein
  */

class DecodeOptionalType(name: Optional[IDecodeName], ns: DecodeNamespace, info: Optional[String])
  extends AbstractDecodeType(name, ns, info) with DecodeGenericType {
  override def getTypeParameters: util.List[Optional[IDecodeName]] = DecodeOptionalType.typeParameters
}

object DecodeOptionalType {
  private val typeParameters: util.List[Optional[IDecodeName]] = Seq[Optional[IDecodeName]](Optional.of(DecodeName.newFromMangledName("T"))).asJava
  val NAME: String = "optional"
  val MANGLED_NAME: DecodeName = DecodeName.newFromMangledName(NAME)
}
