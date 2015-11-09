package ru.mipt.acsl.decode.model.domain.impl

import ru.mipt.acsl.decode.model.domain._

/**
  * @author Artem Shein
  */
class TokenWalker(val token: Either[String, Int]) extends DecodeTypeVisitor[Option[DecodeType]] {
  override def visit(primitiveType: DecodePrimitiveType) = None

  override def visit(nativeType: DecodeNativeType) = None

  override def visit(subType: DecodeSubType) = subType.baseType.obj.accept(this)

  override def visit(enumType: DecodeEnumType) = None

  override def visit(arrayType: DecodeArrayType) = {
    if (!token.isRight)
      sys.error("invalid token")
    Some(arrayType.baseType.obj)
  }

  override def visit(structType: DecodeStructType) = {
    if (token.isLeft)
      sys.error("invalid token")
    val name = token.left
    Some(structType.fields.find(_.name.asString == name)
      .getOrElse({
      throw new AssertionError(
        String.format("Field '%s' not found in struct '%s'", name, structType))
    }).fieldType.obj)
  }

  override def visit(typeAlias: DecodeAliasType) =  typeAlias.baseType.obj.accept(this)

  override def visit(genericType: DecodeGenericType) = None

  override def visit(genericTypeSpecialized: DecodeGenericTypeSpecialized) = None
}