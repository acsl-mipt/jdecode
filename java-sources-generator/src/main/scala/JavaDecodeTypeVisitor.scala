package ru.mipt.acsl.decode.java.generator

import com.google.common.base.CaseFormat

import ru.mipt.acsl.decode.model.domain._
import ru.mipt.acsl.generator.java.ast.{JavaTypeApplication, JavaType}

object JavaDecodeTypeVisitor {
  def classNameFromTypeName(typeName: String): String = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, typeName)

  def classNameFromArrayType(arrayType: DecodeArrayType): String = "Array" + (
    if (arrayType.isFixedSize)
      arrayType.size.minLength
    else if (arrayType.size.maxLength == 0)
      ""
    else
      arrayType.size.minLength + "_" + arrayType.size.maxLength)

  def getJavaTypeForDecodeType(t: DecodeType, genericUse: Boolean): JavaType = t.accept(new JavaDecodeTypeVisitor(genericUse))
}

class JavaDecodeTypeVisitor(val genericUse: Boolean) extends DecodeTypeVisitor[JavaType] {

  def visit(primitiveType: DecodePrimitiveType) = {
    (primitiveType.kind, primitiveType.bitLength, genericUse) match
    {
      case (TypeKind.Int, 8, true) => JavaType.Std.BYTE
      case (TypeKind.Int, 8, false) => JavaType.Primitive.BYTE
      case (TypeKind.Int, 16, true) => JavaType.Std.SHORT
      case (TypeKind.Int, 16, false) => JavaType.Primitive.SHORT
      case (TypeKind.Int, 32, true) => JavaType.Std.INTEGER
      case (TypeKind.Int, 32, false) => JavaType.Primitive.INT
      case (TypeKind.Int, 64, true) => JavaType.Std.LONG
      case (TypeKind.Int, 16, false) => JavaType.Primitive.LONG
      case (TypeKind.Uint, 8, true) => JavaType.Std.SHORT
      case (TypeKind.Uint, 8, false) => JavaType.Primitive.SHORT
      case (TypeKind.Uint, 16, true) => JavaType.Std.INTEGER
      case (TypeKind.Uint, 16, false) => JavaType.Primitive.INT
      case (TypeKind.Uint, 32, true) => JavaType.Std.LONG
      case (TypeKind.Uint, 32, false) => JavaType.Primitive.LONG
      case (TypeKind.Uint, 64, _) => JavaType.Std.BIG_INTEGER
      case (TypeKind.Float, 32, true) => JavaType.Std.FLOAT
      case (TypeKind.Float, 32, false) => JavaType.Primitive.FLOAT
      case (TypeKind.Float, 64, true) => JavaType.Std.DOUBLE
      case (TypeKind.Float, 64, false) => JavaType.Primitive.DOUBLE
      case (TypeKind.Float, _, true) => JavaType.Std.BOOLEAN
      case (TypeKind.Float, _, false) => JavaType.Primitive.BOOLEAN
      case _ => throw new AssertionError()
    }
  }

  def visit(nativeType: DecodeNativeType) = new JavaTypeApplication("decode.Ber")

  def visit(subType: DecodeSubType) = new JavaTypeApplication(subType.namespace.fqn.asString() + "." + JavaDecodeTypeVisitor.classNameFromTypeName(
      // FIXME: handle Option
      subType.optionName.get.asString()))

  def visit(enumType: DecodeEnumType) = new JavaTypeApplication(enumType.namespace.fqn.asString() + "." +
      // FIXME: handle Option
    JavaDecodeTypeVisitor.classNameFromTypeName(enumType.optionName.get.asString()))

  def visit(arrayType: DecodeArrayType) = new JavaTypeApplication(
      arrayType.namespace.fqn.asString() + "." + JavaDecodeTypeVisitor.classNameFromArrayType(arrayType),
    JavaDecodeTypeVisitor.getJavaTypeForDecodeType(arrayType.baseType.obj, genericUse = true))

  def visit(structType: DecodeStructType) = new JavaTypeApplication(structType.namespace.fqn.asString() + "." +
      // FIXME: handle Option
    JavaDecodeTypeVisitor.classNameFromTypeName(structType.optionName.get.asString()))

  def visit(typeAlias: DecodeAliasType) = JavaDecodeTypeVisitor.getJavaTypeForDecodeType(typeAlias.baseType.obj, genericUse)

  def visit(genericType: DecodeGenericType) = throw new AssertionError("not implemented")

  def visit(genericTypeSpecialized: DecodeGenericTypeSpecialized) = throw new AssertionError("not implemented")
}