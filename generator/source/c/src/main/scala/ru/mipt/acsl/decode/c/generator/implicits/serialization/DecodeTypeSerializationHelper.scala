package ru.mipt.acsl.decode.c.generator.implicits.serialization

import ru.mipt.acsl.decode.c.generator.CSourceGenerator._
import ru.mipt.acsl.decode.c.generator.implicits._
import ru.mipt.acsl.decode.model.domain.impl.types.{AliasType, ArrayType, EnumType, GenericTypeSpecialized, HasBaseType, NativeType, PrimitiveTypeInfo, SubType, TypeKind}
import ru.mipt.acsl.decode.model.domain.types.{DecodeType, StructType}
import ru.mipt.acsl.generator.c.ast.implicits._
import ru.mipt.acsl.generator.c.ast.{CAstElements => _, _}

/**
  * @author Artem Shein
  */
private[generator] case class DecodeTypeSerializationHelper(t: DecodeType) {

  def serializeMethodName: String = t.methodName(typeSerializeMethodName)

  def deserializeMethodName: String = t.methodName(typeDeserializeMethodName)

  private def trySerializeFuncCall(src: CExpression): CFuncCall = serializeFuncCall(src)._try

  private def tryDeserializeFuncCall(dest: CExpression): CFuncCall = deserializeFuncCall(dest)._try

  private def deserializeFuncCall(dest: CExpression): CFuncCall = t.methodName(typeDeserializeMethodName).call(dest, reader.v)

  private def callCodeForBer(methodNamePart: String, exprs: CExpression*): CExpression =
    photonBerTypeName.methodName(methodNamePart).call(exprs: _*)

  def serializeBerCallCode(src: CExpression): CExpression = callCodeForBer(typeSerializeMethodName, src, writer.v)

  def deserializeBerCallCode(dest: CExpression): CExpression = callCodeForBer(typeDeserializeMethodName, dest, reader.v)

  def deserializeCallCode(dest: CExpression): CExpression = t match {
    case _: ArrayType | _: StructType => deserializeFuncCall(dest)
    case t: NativeType if t.isPrimitive =>
      CAssign(CDeref(dest), callCodeForPrimitiveType(t.primitiveTypeInfo, dest, photonReaderTypeName, "Read", reader.v))
    case n: NativeType => n.isVaruintType match {
      case true => callCodeForBer(typeDeserializeMethodName, dest, reader.v)
      case _ => sys.error(s"not implemented for $n")
    }
    case t: HasBaseType =>
      val baseType = t.baseType
      baseType.deserializeCallCode(dest.cast(baseType.cType.ptr))
    case _ => sys.error(s"not implemented for $t")
  }

  def serializeCallCode(src: CExpression): CExpression = t match {
    case _: ArrayType | _: StructType => serializeFuncCall(src)
    case t: NativeType if t.isPrimitive =>
      callCodeForPrimitiveType(t.primitiveTypeInfo, src, photonWriterTypeName, "Write", writer.v, src)
    case t: NativeType => t.isVaruintType match {
      case true => callCodeForBer(typeSerializeMethodName, src, writer.v)
      case _ => sys.error(s"not implemented for $t")
    }
    case t: HasBaseType =>
      t.baseType.serializeCallCode(src)
    case _ =>
      serializeFuncCall(src)
  }

  def callCodeForPrimitiveType(t: PrimitiveTypeInfo, src: CExpression, typeName: String, methodPrefix: String,
                               exprs: CExpression*): CFuncCall = {
    import TypeKind._
    typeName.methodName(methodPrefix + ((t.kind, t.bitLength) match {
      case (_, 8) => "Uint8"
      case (Bool, 16) | (Uint, 16) => "Uint16Le"
      case (Bool, 32) | (Uint, 32) => "Uint32Le"
      case (Bool, 64) | (Uint, 64) => "Uint64Le"
      case (Int, 16) => "Int16Le"
      case (Int, 32) => "Int32Le"
      case (Int, 64) => "Int64Le"
      case (Float, 32) => "Float32Le"
      case (Float, 64) => "Float64Le"
      case _ => sys.error(s"not implemented $t")
    })).call(exprs: _*)
  }

  private def serializeFuncCall(src: CExpression): CFuncCall = t.methodName(typeSerializeMethodName).call(src, writer.v)

  private def serializeGenericTypeSpecializedCode(t: GenericTypeSpecialized, src: CExpression): CAstElements = {
    val isSmall = t.isSmall
    t.genericType match {
      case or if or.isOrType =>
        val tagField = if (isSmall) src.dot(tagVar) else src -> tagVar
        photonBerTypeName.methodName(typeSerializeMethodName).call(tagField, writer.v)._try.line +:
          Seq(CIndent, CSwitch(tagField, t.genericTypeArguments.zipWithIndex.map { case (omp, idx) =>
            CCase(CIntLiteral(idx), omp.map { mp =>
              val valueVar = ("_" + (idx + 1))._var
              Seq(mp.serializeCallCode(
                mapIfNotSmall(if (isSmall) src.dot(valueVar) else src -> valueVar, mp,
                  (expr: CExpression) => expr.ref)).line, CIndent, CBreak, CSemicolon, CEol)
            }.getOrElse {
              Seq(CStatementLine(CBreak, CSemicolon))
            })
          }, default = CStatements(CReturn(invalidValue))), CEol)
      case optional if optional.isOptionalType =>
        val flagField = if (isSmall) src.dot(flagVar) else src -> flagVar
        photonBerTypeName.methodName(typeSerializeMethodName).call(
          flagField, writer.v)._try.line +:
          Seq(CIndent, CIf(flagField, CEol +:
            t.genericTypeArguments.head.getOrElse {
              sys.error("wtf")
            }.serializeCode(
              if (isSmall) src.dot(valueVar) else src -> valueVar)))
      case _ => sys.error(s"not implemented $t")
    }
  }

  private def deserializeGenericTypeSpecializedCode(t: GenericTypeSpecialized, dest: CExpression): CAstElements =
    t.genericType match {
      case or if or.isOrType =>
        photonBerTypeName.methodName(typeDeserializeMethodName).call((dest -> tagVar).ref, reader.v)._try.line +:
          Seq(CIndent, CSwitch(dest -> tagVar, t.genericTypeArguments.zipWithIndex.map { case (omp, idx) =>
            CCase(CIntLiteral(idx), omp.map { mp =>
              Seq(mp.deserializeCallCode((dest -> ("_" + (idx + 1))._var).ref).line,
                CIndent, CBreak, CSemicolon, CEol)
            }.getOrElse {
              Seq(CStatementLine(CBreak, CSemicolon))
            })
          }, default = CStatements(CReturn(invalidValue))), CEol)
      case optional if optional.isOptionalType =>
        photonBerTypeName.methodName(typeDeserializeMethodName).call(
          (dest -> flagVar).ref.cast(CTypeApplication(photonBerTypeName).ptr), reader.v)._try.line +:
          Seq(CIndent, CIf(dest -> flagVar, CEol +:
            t.genericTypeArguments.head.getOrElse {
              sys.error("wtf")
            }.deserializeCode((dest -> valueVar).ref)))
      case _ => sys.error(s"not implemented $t")
    }

  private val berSizeOf = "sizeof".call(berType)

  def abstractMinSizeExpr(forceParens: Boolean): Option[CExpression] = abstractMinSizeExpr(Set.empty, forceParens)

  def abstractMinSizeExpr(enclosingTypes: Set[DecodeType], forceParens: Boolean): Option[CExpression] = enclosingTypes.contains(t) match {
    case true => None
    case _ =>
      val extendedEnclosingTypes = enclosingTypes + t
      t match {
        case t: NativeType => Some("sizeof".call(t.cType))
        case t: AliasType => t.baseType.abstractMinSizeExpr(extendedEnclosingTypes, forceParens)
        case t: SubType => t.baseType.abstractMinSizeExpr(extendedEnclosingTypes, forceParens)
        case t: StructType =>
          sumExprs(t.fields.map(f => f.typeUnit.t.abstractMinSizeExpr(extendedEnclosingTypes, forceParens = false)), forceParens)
        case array: ArrayType =>
          val baseType = array.baseType
          val baseTypeMinSizeExprOption = baseType.abstractMinSizeExpr(extendedEnclosingTypes, forceParens = false)
          val minSize = array.size.min
          if (baseTypeMinSizeExprOption.isDefined && minSize != 0) {
            val baseTypeMinSizeExpr = baseTypeMinSizeExprOption.get
            Some(CPlus(berSizeOf,
              if (minSize == 1)
                baseTypeMinSizeExpr
              else
                CMul(commentBeginEnd(baseTypeMinSizeExpr, baseType.cName), CULongLiteral(minSize))))
              .map(expr => if (forceParens) CParens(expr) else expr)
          } else {
            Some(berSizeOf)
          }
        case _ => sys.error(s"not implemented for $t")
      }
  }

  private def commentBeginEnd(expr: CExpression, typeName: String): CCommentedExpression =
    CCommented(expr, typeName + "{", "}" + typeName)

  private def sumExprs(exprs: Seq[Option[CExpression]], forceParens: Boolean): Option[CExpression] =
    exprs.foldLeft[Option[CExpression]](None)(sumExprs).map(expr => if (forceParens) CParens(expr) else expr)

  private def sumExprs(l: Option[CExpression], r: Option[CExpression]): Option[CExpression] =
    l.map(lExpr =>
      r.map(rExpr => CPlus(lExpr, rExpr))
        .getOrElse(lExpr))
      .orElse(r)

  def concreteMinSizeExpr(src: CExpression, isPtr: Boolean): Option[CExpression] = t match {
    case struct: StructType =>
      sumExprs(
        struct.fields.map(f =>
          f.typeUnit.t.concreteMinSizeExpr(src.dotOrArrow(f.cName._var, isDot = !isPtr), isPtr = false)),
        forceParens = false)
        .map(commentBeginEnd(_, struct.cName))
    case array: ArrayType =>
      val baseType = array.baseType
      baseType.abstractMinSizeExpr(forceParens = true).map(rExpr =>
        CMul(src.dotOrArrow(size.v, isDot = !isPtr),
          commentBeginEnd(rExpr, baseType.cName)))
    case _: SubType | _: AliasType | _: GenericTypeSpecialized => None // todo: yes you can
    case t: EnumType => t.baseType.concreteMinSizeExpr(src, isPtr)
    case _ => abstractMinSizeExpr(forceParens = false)
  }

  private def writerSizeCheckCode(src: CExpression) = concreteMinSizeExpr(src, !t.isSmall).map { sizeExpr =>
    Seq(CIndent, CIf(CLess("PhotonWriter_WritableSize".call(writer.v), sizeExpr),
      Seq(CEol, CReturn("PhotonResult_NotEnoughSpace"._var).line)))
  }.getOrElse(Seq.empty)

  private def readerSizeCheckCode(dest: CExpression) = concreteMinSizeExpr(dest, isPtr = true).map { sizeExpr =>
    Seq(CIndent, CIf(CLess("PhotonReader_ReadableSize".call(reader.v), sizeExpr),
      Seq(CEol, CReturn("PhotonResult_NotEnoughData"._var).line)))
  }.getOrElse(Seq.empty)

  def serializeCode: CAstElements = serializeCode(selfVar)

  def serializeCode(src: CExpression): CAstElements = t match {
    case struct: StructType => writerSizeCheckCode(src) ++ struct.fields.flatMap { f =>
      val fType = f.typeUnit.t
      val fVar = f.cName._var
      Seq(fType.serializeCallCode(src.dotOrArrow(fVar, struct.isSmall).refIfNotSmall(fType)).line)
    }
    case array: ArrayType =>
      writerSizeCheckCode(src) ++ src.serializeCodeForArraySize(array) ++ array.serializeCodeForArrayElements(src)
    case alias: AliasType => Seq(alias.baseType.serializeCallCode(src).line)
    case s: SubType => Seq(s.baseType.serializeCallCode(src).line)
    case enum: EnumType => Seq(enum.baseType.serializeCallCode(src).line)
    case native: NativeType if native.isPrimitive => Seq(native.serializeCallCode(src).line)
    case native: NativeType => Seq(native.serializeCallCode(src).line)
    case gts: GenericTypeSpecialized => serializeGenericTypeSpecializedCode(gts, src)
    case _ => sys.error(s"not implemented for $t")
  }

  def deserializeCode: CAstElements = deserializeCode(selfVar)

  def deserializeCode(dest: CExpression): CAstElements = t match {
    case t: StructType => /*todo: implement or remove: readerSizeCheckCode(dest) ++*/ t.fields.flatMap(f =>
      Seq(f.typeUnit.t.deserializeCallCode((dest -> f.cName._var).ref).line))
    case t: ArrayType =>
      dest.deserializeCodeForArraySize(t) ++ readerSizeCheckCode(dest) ++ t.deserializeCodeForArrayElements(dest)
    case t: AliasType => t.baseType.deserializeCode(dest)
    case t: HasBaseType =>
      val baseType = t.baseType
      Seq(baseType.deserializeCallCode(dest.cast(baseType.cType.ptr)).line)
    case _: NativeType => Seq(t.deserializeCallCode(dest).line)
    case t: GenericTypeSpecialized => deserializeGenericTypeSpecializedCode(t, dest)
    case _ => sys.error(s"not implemented for $t")
  }

}
