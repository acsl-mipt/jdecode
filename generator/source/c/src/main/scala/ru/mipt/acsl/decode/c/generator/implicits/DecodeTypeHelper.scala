package ru.mipt.acsl.decode.c.generator.implicits

import ru.mipt.acsl.decode.c.generator.CSourceGenerator._
import ru.mipt.acsl.decode.model.domain.impl.naming.{Fqn, Namespace}
import ru.mipt.acsl.decode.model.domain.impl.types.{ArrayType, EnumType, GenericType, GenericTypeSpecialized, HasBaseType, NativeType}
import ru.mipt.acsl.decode.model.domain.types.{DecodeType, StructType}
import ru.mipt.acsl.generator.c.ast._

import scala.collection.{immutable, mutable}

/**
  * @author Artem Shein
  */
private[generator] case class DecodeTypeHelper(t: DecodeType) {

  private val orFqn = Fqn.newFromSource("decode.or")

  private val optionalFqn = Fqn.newFromSource("decode.optional")

  def byteSize: Int = byteSize(immutable.Set.empty)

  def isOrType: Boolean = t.fqn.equals(orFqn)

  def isOptionalType: Boolean = t.fqn.equals(optionalFqn)

  def dotOrArrow(expr: CExpression, exprRight: CExpression): CExpression = t.isSmall match {
    case true => expr.dot(exprRight)
    case _ => expr -> exprRight
  }

  def byteSize(enclosingTypes: Set[DecodeType]): Int = enclosingTypes.contains(t) match {
    case true => PtrSize
    case _ =>
      val extendedEclosingTypes = enclosingTypes + t
      t match {
        case t: NativeType if t.isPrimitive => (t.primitiveTypeInfo.bitLength / 8).toInt
        case t: GenericTypeSpecialized => t.genericType match {
          case optional if optional.isOptionalType => 1 + t.genericTypeArguments.head.get.byteSize(extendedEclosingTypes)
          case or if or.isOrType => 1 + t.genericTypeArguments.map(
            _.map(_.byteSize(extendedEclosingTypes)).getOrElse(0)).max
          case _ => sys.error(s"not implemented for $t")
        }
        case n: NativeType => n.isVaruintType match {
          case true => VaruintByteSize
          case _ => sys.error(s"not implemented for $n")
        }
        case t: ArrayType => t.size.max match {
          case 0 => VaruintByteSize + PtrSize
          case _ => (t.size.max * t.baseType.byteSize(extendedEclosingTypes)).toInt
        }
        case t: StructType => t.fields.map(_.typeUnit.t.byteSize(extendedEclosingTypes)).sum
        case t: HasBaseType => t.baseType.byteSize(extendedEclosingTypes)
        case _ => sys.error(s"not implemented for $t")
      }
  }

  def methodName(name: String): String = t.prefixedCTypeName.methodName(name)

  def isSmall: Boolean = t.byteSize <= 16

  def prefixedCTypeName: String = "PhotonGt" + cTypeName

  def cTypeName: String = t match {
    case t: ArrayType =>
      val baseCType = t.baseType.cTypeName
      val min = t.size.min
      val max = t.size.max
      "Arr" + baseCType + ((t.isFixedSize, min, max) match {
        case (true, 0, _) | (false, 0, 0) => ""
        case (true, _, _) => s"Fixed$min"
        case (false, 0, _) => s"Max$max"
        case (false, _, 0) => s"Min$min"
        case (false, _, _) => s"Min${min}Max$max"
      })
    case t: GenericTypeSpecialized =>
      t.genericType.cTypeName +
        t.genericTypeArguments.map(_.map(_.cTypeName).getOrElse("Void")).mkString
    case named => named.name.asMangledString.lowerUnderscore2UpperCamel
  }

  def cMethodReturnType: CType = if (t.isSmall) t.cType else t.cType.ptr.const

  def cMethodReturnParameters: Seq[CFuncParam] = Seq.empty //if (t.isSmall) Seq.empty else Seq(CFuncParam("result", t.cType.ptr))

  def cType: CType = CTypeApplication(t.prefixedCTypeName)

  def isNative = t match {
    case _: NativeType => true
    case _ => false
  }

  def fileName: String = t.prefixedCTypeName

  def isBasedOnEnum: Boolean = t match {
    case _: EnumType => true
    case _: ArrayType => false
    case t: HasBaseType => t.baseType.isBasedOnEnum
    case _ => false
  }

  def isGeneratable: Boolean = t match {
    case _ if isNative => false
    case t: GenericType => false
    case _ => true
  }

  def typeWithDependentTypes: immutable.Set[DecodeType] =
    typeWithDependentTypes(immutable.Set.empty)

  def typeWithDependentTypes(exclude: Set[DecodeType]): immutable.Set[DecodeType] =
    (exclude.contains(t) match {
      case true => Set.empty[DecodeType]
      case _ =>
        val extendedExclude = exclude + t
        t match {
          case t: StructType => t.fields.flatMap(_.typeUnit.t.typeWithDependentTypes(extendedExclude)).toSet
          case t: HasBaseType => t.baseType.typeWithDependentTypes(extendedExclude)
          case t: GenericTypeSpecialized =>
            t.genericTypeArguments.flatMap(_.map(_.typeWithDependentTypes(extendedExclude)).getOrElse(Set.empty)).toSet ++
              t.genericType.typeWithDependentTypes(extendedExclude)
          case _: NativeType | _: GenericType => Set.empty[DecodeType]
          case _ => sys.error(s"not implemented for $t")
        }
    }) + t

  def cTypeDef(cType: CType) = CTypeDefStatement(t.prefixedCTypeName, cType)

  def collectNamespaces(set: mutable.Set[Namespace]) {
    set += t.namespace
    t match {
      case t: HasBaseType => collectNsForType(t.baseType, set)
      case t: StructType => t.fields.foreach(f => collectNsForType(f.typeUnit.t, set))
      case t: GenericTypeSpecialized => t.genericTypeArguments.foreach(_.foreach(collectNsForType(_, set)))
      case _ =>
    }
  }

  def importTypes: Seq[DecodeType] = t match {
    case t: StructType => t.fields.flatMap { f =>
      val t = f.typeUnit.t
      if (t.isNative)
        Seq.empty
      else
        Seq(t)
    }
    case s: GenericTypeSpecialized =>
      s.genericType match {
        case optional if optional.isOptionalType =>
          Seq(s.genericTypeArguments.head.getOrElse {
            sys.error("invalid optional types")
          })
        case or if or.isOrType =>
          s.genericTypeArguments.flatMap(_.map(p => Seq(p)).getOrElse(Seq.empty))
      }
    case t: HasBaseType =>
      if (t.baseType.isNative)
        Seq.empty
      else
        Seq(t.baseType)
    case _ => Seq.empty
  }

}
