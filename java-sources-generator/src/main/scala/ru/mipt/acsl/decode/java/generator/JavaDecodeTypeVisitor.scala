package ru.mipt.acsl.decode.java.generator

import com.google.common.base.CaseFormat
import ru.mipt.acsl.decode.model.domain._
import ru.mipt.acsl.decode.model.domain.impl.`type`.{DecodeBerType, DecodeOptionalType, DecodeOrType}
import ru.mipt.acsl.generator.java.ast.{JavaType, JavaTypeApplication}

/**
  * @author Artem Shein
  */
object JavaDecodeTypeVisitor {
  def classNameFromTypeName(typeName: String): String = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, typeName)

  def classNameFromArrayType(arrayType: DecodeArrayType): String = "Array" + (
    if (arrayType.isFixedSize)
      arrayType.size.minLength
    else if (arrayType.size.maxLength == 0)
      ""
    else
      arrayType.size.minLength + "_" + arrayType.size.maxLength)

  def getJavaTypeForDecodeType(t: DecodeType, genericUse: Boolean): JavaType = t match {
    case t: DecodePrimitiveType => (t.kind, t.bitLength, genericUse) match {
      case (TypeKind.Int, 8, true) => JavaType.Std.BYTE
      case (TypeKind.Int, 8, false) => JavaType.Primitive.BYTE
      case (TypeKind.Int, 16, true) => JavaType.Std.SHORT
      case (TypeKind.Int, 16, false) => JavaType.Primitive.SHORT
      case (TypeKind.Int, 32, true) => JavaType.Std.INTEGER
      case (TypeKind.Int, 32, false) => JavaType.Primitive.INT
      case (TypeKind.Int, 64, true) => JavaType.Std.LONG
      case (TypeKind.Int, 64, false) => JavaType.Primitive.LONG
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
      case _ => sys.error("invalid bit length")
    }
    case t: DecodeArrayType => new JavaTypeApplication(t.namespace.fqn.asString() + "." + JavaDecodeTypeVisitor.classNameFromArrayType(t),
      JavaDecodeTypeVisitor.getJavaTypeForDecodeType(t.baseType.obj, genericUse = true))
    case t: DecodeAliasType => getJavaTypeForDecodeType(t.baseType.obj, genericUse)
    case t: DecodeNativeType if t.isInstanceOf[DecodeBerType] => new JavaTypeApplication("decode.ber")
    case t: DecodeNativeType if t.isInstanceOf[DecodeOrType] => new JavaTypeApplication("decode.or")
    case t: DecodeNativeType if t.isInstanceOf[DecodeOptionalType] => new JavaTypeApplication("decode.optional")
    case t: DecodeOptionNamed => new JavaTypeApplication(t.namespace.fqn.asString() + "." + JavaDecodeTypeVisitor.classNameFromTypeName(
      // FIXME: handle Option
      t.optionName.get.asString()))
    case _ => sys.error("not implemented")
  }
}
