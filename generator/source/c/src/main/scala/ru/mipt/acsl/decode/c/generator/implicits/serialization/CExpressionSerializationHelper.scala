package ru.mipt.acsl.decode.c.generator.implicits.serialization

import ru.mipt.acsl.decode.c.generator.CSourceGenerator._
import ru.mipt.acsl.generator.c.ast.{CExpression, CFuncCall, CStatements}
import ru.mipt.acsl.generator.c.ast.implicits._
import ru.mipt.acsl.decode.c.generator.implicits._
import ru.mipt.acsl.decode.model.types.GenericTypeSpecialized

/**
  * @author Artem Shein
  */
private[generator] case class CExpressionSerializationHelper(expr: CExpression) {

  def serializeCodeForArraySize(t: GenericTypeSpecialized): CAstElements = {
    assert(t.isArray) // fixme: static typing
    val sizeExpr = t.isSmall match {
      case false => expr -> size.v
      case _ => expr.dot(size.v)
    }
    CStatements(sizeExpr.serializeCallCodeForArraySize)
  }

  def serializeCallCodeForArraySize: CFuncCall =
    photonBerTypeName.methodName(typeSerializeMethodName).call(Seq(expr, writer.v): _*)._try

  def deserializeCodeForArraySize(t: GenericTypeSpecialized): CAstElements = {
    assert(t.isArray) // fixme: static typing
    val sizeExpr = expr -> size.v
    CStatements(sizeExpr.deserializeCallCodeForArraySize)
  }

  def deserializeCallCodeForArraySize: CFuncCall =
    photonBerTypeName.methodName(typeDeserializeMethodName).call(Seq(expr.ref, reader.v): _*)._try
}
