package ru.mipt.acsl.decode.c.generator.implicits

import ru.mipt.acsl.decode.c.generator.CSourceGenerator._
import ru.mipt.acsl.decode.model.domain.component.message._
import ru.mipt.acsl.decode.model.domain.impl.types.ArrayType
import ru.mipt.acsl.decode.model.domain.types.DecodeType
import ru.mipt.acsl.generator.c.ast.{CAstElements => _, _}

/**
  * @author Artem Shein
  */
package object serialization {

  val photonWriterTypeName = "PhotonWriter"
  val photonReaderTypeName = "PhotonReader"
  val typeSerializeMethodName = "Serialize"
  val typeDeserializeMethodName = "Deserialize"
  val tagVar: CVar = "tag"._var
  val flagVar: CVar = "flag"._var
  val valueVar: CVar = "value"._var
  val invalidValue = CVar("PhotonResult_InvalidValue")
  val dataVar = CVar("data")

  implicit def decodeType2DecodeTypeSerializationHelper(decodeType: DecodeType): DecodeTypeSerializationHelper =
    DecodeTypeSerializationHelper(decodeType)

  implicit def arrayType2arrayTypeSerializationHelper(arrayType: ArrayType): ArrayTypeSerializationHelper =
    ArrayTypeSerializationHelper(arrayType)

  implicit def messageParameter2MessageParameterSerializationHelper(messageParameter: MessageParameter)
  : MessageParameterSerializationHelper = MessageParameterSerializationHelper(messageParameter)

  implicit def cExpression2CExpressionSerializationHelper(cExpr: CExpression): CExpressionSerializationHelper =
    CExpressionSerializationHelper(cExpr)

  implicit class CVarSerializationHelper(val v: CVar) {

    def serializeBer: CFuncCall = photonBerTypeName.methodName(typeSerializeMethodName).call(v, writer.v)

  }

}
