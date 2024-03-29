package ru.mipt.acsl.decode.java.generator

import com.google.common.base.CaseFormat
import ru.mipt.acsl.decode.model.types._
import ru.mipt.acsl.decode.model.naming._
import ru.mipt.acsl.decode.model.types.DecodeType
import ru.mipt.acsl.generator.java.ast.{JavaType, JavaTypeApplication}

/**
  * @author Artem Shein
  */
object JavaDecodeTypeVisitor {
  def classNameFromTypeName(typeName: String): String = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, typeName)

  /*def classNameFromArrayType(arrayType: ArrayType): String = "Array" + (
    if (arrayType.isFixedSize)
      arrayType.size.min
    else if (arrayType.size.max == 0)
      ""
    else
      arrayType.size.min + "_" + arrayType.size.max)*/

  def getJavaTypeForDecodeType(t: DecodeType, genericUse: Boolean): JavaType = t match {
    case t: NativeType =>
      val p = PrimitiveTypeInfo.typeInfoByFqn(t.fqn.get())
      (p.kind, p.bitLength, genericUse) match {
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
    /*case t: ArrayType => new JavaTypeApplication(t.namespace.fqn.asMangledString + "." + JavaDecodeTypeVisitor.classNameFromArrayType(t),
      JavaDecodeTypeVisitor.getJavaTypeForDecodeType(t.baseType, genericUse = true))*/
    //case t: Alias => getJavaTypeForDecodeType(t.obj, genericUse)
    //case t: HasOptionName => new JavaTypeApplication(t.namespace.fqn.asMangledString + "." + JavaDecodeTypeVisitor.classNameFromTypeName(
      // FIXME: handle Option
    //  t.optionName.get.asMangledString))
    case _ => sys.error("not implemented")
  }
}
