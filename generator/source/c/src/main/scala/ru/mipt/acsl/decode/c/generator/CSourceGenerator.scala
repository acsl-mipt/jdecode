package ru.mipt.acsl.decode.c.generator

import java.io
import java.io.{File, FileOutputStream, OutputStreamWriter}

import com.google.common.base.CaseFormat
import com.typesafe.scalalogging.LazyLogging
import resource._
import ru.mipt.acsl.decode.model.domain.impl.component.message._
import ru.mipt.acsl.decode.model.domain.impl.component.{Command, Component}
import ru.mipt.acsl.decode.model.domain.impl.naming.{ElementName, Fqn, Namespace}
import ru.mipt.acsl.decode.model.domain.impl.types.{AliasType, ArrayType, DecodeType, EnumType, GenericType, GenericTypeSpecialized, HasBaseType, NativeType, PrimitiveTypeInfo, StructField, StructType, SubType, TypeKind}
import ru.mipt.acsl.decode.model.domain.pure.HasOptionId
import ru.mipt.acsl.decode.model.domain.pure.component.messages.{MessageParameter, StatusMessage, TmMessage}
import ru.mipt.acsl.decode.model.domain.pure.expr.{ConstExpr, IntLiteral}
import ru.mipt.acsl.decode.model.domain.pure.naming.HasName
import ru.mipt.acsl.generation.Generator
import ru.mipt.acsl.generator.c.ast._
import ru.mipt.acsl.generator.c.ast.implicits._

import scala.collection.{immutable, mutable}
import scala.io.Source

class CSourceGenerator(val config: CGeneratorConfiguration) extends Generator[CGeneratorConfiguration] with LazyLogging {

  import CSourceGenerator._

  override def getConfiguration: CGeneratorConfiguration = config

  override def generate(): Unit = {
    val component = config.registry.component(config.rootComponentFqn).getOrElse(
      sys.error(s"component not found ${config.rootComponentFqn}"))
    generateRootComponent(component)
  }

  private def ensureDirForNsExists(ns: Namespace): io.File = {
    val dir = dirForNs(ns)
    if (!(dir.exists() || dir.mkdirs()))
      sys.error(s"Can't create directory ${dir.getAbsolutePath}")
    dir
  }

  private def generateNs(ns: Namespace): Unit = {
    val nsDir = ensureDirForNsExists(ns)
    ns.subNamespaces.foreach(generateNs)
    val typesHeader = CAstElements()
    ns.types.foreach(t => generateTypeSeparateFiles(t, nsDir))
    val fileName: String = "types" + headerExt
    new io.File(nsDir, fileName).writeIfNotEmptyWithComment(typesHeader.protectDoubleInclude(dirPathForNs(ns) +
      io.File.separator + fileName), s"Types of ${ns.fqn.asMangledString} namespace")
  }

  private def generateType(t: DecodeType, nsDir: io.File): (CAstElements, CAstElements) = {
    var (h, c): (CAstElements, CAstElements) = t match {
      case t: NativeType if t.isPrimitive => (Seq(t.cTypeDef(t.cType)), Seq.empty)
      case t: NativeType => (Seq(t.cTypeDef(CVoidType.ptr)), Seq.empty)
      case t: SubType => (Seq(t.cTypeDef(t.baseType.cType)), Seq.empty)
      case t: EnumType =>
        val prefixedEnumName = upperCamelCaseToUpperUnderscore(t.prefixedCTypeName)
        (Seq(t.cTypeDef(CEnumTypeDef(t.allConstants.toSeq.sortBy(_.value.toInt).map(c =>
          CEnumTypeDefConst(prefixedEnumName + "_" + c.name.asMangledString, c.value.toInt))))),
          Seq.empty)
      case t: GenericType =>
        (CAstElements(), CAstElements())
      case t: GenericTypeSpecialized => t.genericType match {
        case optional if optional.name.equals(ElementName.newFromMangledName("optional")) =>
          require(t.genericTypeArguments.size == 1)
          val head = t.genericTypeArguments.head.getOrElse {
            sys.error("wtf")
          }
          head match {
            // TODO: implement or remove
            // case h if h.isBasedOnEnum => (Seq(t.cTypeDef(head.obj.cType)), Seq.empty)
            case _ => (Seq(t.cTypeDef(CStructTypeDef(Seq(
              CStructTypeDefField("value", t.genericTypeArguments.head.get.cType),
              CStructTypeDefField("flag", b8Type))))), Seq.empty)
          }
        case or if or.name.equals(ElementName.newFromMangledName("or")) =>
          var index = 0
          (Seq(t.cTypeDef(CStructTypeDef(t.genericTypeArguments.flatMap { ot =>
            index += 1
            ot.map(t => Seq(CStructTypeDefField("_" + index, t.cType))).getOrElse(Seq.empty)
          } :+ tag.field))), Seq.empty)
      }
      case t: ArrayType =>
        val arrayType: CType = t.size.max match {
          case 0 => t.baseType.cType.ptr
          case _ => CArrayType(t.baseType.cType, t.size.max, dataVar.name)
        }
        val typeDef = CTypeDefStatement(t.prefixedCTypeName, CStructTypeDef(Seq(
          CStructTypeDefField(size.name, sizeTType),
          CStructTypeDefField(dataVar.name, arrayType))))
        val defineNamePrefix = upperCamelCaseToUpperUnderscore(t.prefixedCTypeName)
        (Seq(CDefine(defineNamePrefix + "_MIN_SIZE", t.size.min.toString), CEol,
          CDefine(defineNamePrefix + "_MAX_SIZE", t.size.max.toString), CEol, CEol, typeDef), CAstElements())
      case t: StructType => (Seq(t.cTypeDef(CStructTypeDef(t.fields.map(f =>
        CStructTypeDefField(f.name.asMangledString, f.typeUnit.t.cType))))), Seq.empty)
      case t: AliasType =>
        if (isAliasNameTheSame(t))
          (Seq.empty, Seq.empty)
        else
          (Seq(t.cTypeDef(t.baseType.cType)), Seq.empty)
      case _ => sys.error(s"not implemented $t")
    }

    if (h.nonEmpty) {
      val importTypes = t.importTypes
      val imports = CAstElements(importTypes.filterNot(_.isNative).flatMap(t => CInclude(relPathForType(t)).eol): _*)

      val selfType = t.cType
      val serializeMethod = CFuncImpl(CFuncDef(t.serializeMethodName, resultType,
        Seq(CFuncParam(selfVar.name, mapIfNotSmall(selfType, t, (ct: CType) => ct.ptr.const)), writer.param)),
        t.serializeCode :+ CReturn(resultOk).line)
      val deserializeMethod = CFuncImpl(CFuncDef(t.deserializeMethodName, resultType,
        Seq(CFuncParam(selfVar.name, selfType.ptr), reader.param)), t.deserializeCode :+ CReturn(resultOk).line)

      h = h ++ Seq(CEol, serializeMethod.definition, CEol, CEol, deserializeMethod.definition)
      c = c ++ Seq(CEol, serializeMethod, CEol, CEol, deserializeMethod)

      ((if (imports.nonEmpty) imports.eol else imports) ++ h.externC.eol, c)
    } else {
      (CAstElements(), CAstElements())
    }
  }

  private def generateRootComponent(comp: Component) {
    logger.debug(s"Generating component ${comp.name.asMangledString}")
    provideSources()
    generatePrologueEpilogue()
    config.isSingleton match {
      case true =>
        generateSingleton(comp)
      case _ =>
        sys.error("not implemented")
        val nsSet = mutable.HashSet.empty[Namespace]
        comp.collectNamespaces(nsSet)
        nsSet.foreach(generateNs)
        allComponentsSetForComponent(comp).foreach(generateComponent)
    }
  }

  private def provideSources(): Unit = {
    if (config.sources.isEmpty)
      return
    val dir = new File(config.outputDir, "decode/")
    dir.mkdirs()
    config.sources.foreach(s => new File(dir, new File(s.name).getName).write(s.source))
  }

  private def generatePrologueEpilogue(): Unit = {
    generateFile(config.prologue.isActive, prologuePath, config.prologue.contents)
    generateFile(config.epilogue.isActive, epiloguePath, config.epilogue.contents)
  }

  private def generateFile(isActive: Boolean, filePath: String, contents: Option[String]): Unit = {
    if (isActive) for (c <- contents) {
      val f = new io.File(config.outputDir, filePath)
      f.getParentFile.mkdirs()
      f.write(c)
    }
  }

  private def importStatementsForComponent(comp: Component): CAstElements = {
    val imports = comp.subComponents.flatMap(cr => CInclude(includePathForComponent(cr.component)).eol).to[mutable.Buffer]
    if (imports.nonEmpty)
      imports += CEol
    val types = comp.allTypes.toSeq
    val typeIncludes = types.filterNot(_.isNative).flatMap(t => CInclude(relPathForType(t)).eol)
    imports ++= typeIncludes
    if (typeIncludes.nonEmpty)
      imports += CEol
    imports.to[immutable.Seq]
  }

  private def typeIncludes(component: Component): CAstElements =
    component.allTypes.toSeq.filter(_.isGeneratable).flatMap(t => CInclude(relPathForType(t)).eol)

  private def generateSingleton(component: Component): Unit = {

    component.allTypes.foreach(t => generateTypeSeparateFiles(t, ensureDirForNsExists(t.namespace)))

    val nsDir = ensureDirForNsExists(component.namespace)
    val componentStructName = component.prefixedTypeName
    val (hFile, cFile) = (new File(nsDir, componentStructName + headerExt),
      new File(nsDir, componentStructName + sourcesExt))

    val guidDefines = component.componentDefines

    val methods = component.allMethods ++ component.allSubComponentsMethods

    hFile.write((CEol +: appendPrologueEpilogue((typeIncludes(component) ++
      "USER command implementation functions, MUST BE implemented".comment ++
      component.commandMethodImplDefs.flatMap(m => Seq(CEol, m)).eol ++
      "USER parameter implementation functions, MUST BE implemented".comment ++
      component.parameterMethodImplDefs.flatMap(m => Seq(CEol, m)).eol ++
      "USER other functions, MUST BE implemented".comment ++
      Seq(CFuncDef(component.isEventAllowedMethodName, b8Type, Seq(messageId.param, eventId.param))) ++
      "Component defines".comment.eol ++ guidDefines ++
      "Message ID for component defines".comment.eol ++ component.allMessageDefines ++
      "Command ID for component defines".comment.eol ++ component.allCommandDefines ++
      "Implemented functions".comment ++ methods.map(_.definition).flatMap(m => Seq(CEol, m))).externC.eol))
      .protectDoubleInclude(dirPathForNs(component.namespace) + hFile.getName))

    cFile.write(CInclude(includePathForNsFileName(component.namespace, hFile.getName)).eol.eol ++
      methods.flatMap(m => m.eol.eol))
  }

  private def generateComponent(component: Component) {
    val dir = dirForNs(component.namespace)
    val componentStructName = component.prefixedTypeName
    val hFileName = componentStructName + headerExt
    val (hFile, cFile) = (new io.File(dir, hFileName), new io.File(dir, componentStructName + sourcesExt))
    val imports = importStatementsForComponent(component)
    val componentFunctionTableName = component.functionTableTypeName
    val componentFunctionTableNameStruct = componentFunctionTableName + structNamePostfix
    val forwardFuncTableDecl = CForwardStructDecl(componentFunctionTableNameStruct)
    val componentTypeStructName = componentStructName + structNamePostfix
    val componentTypeForwardDecl = CForwardStructTypeDef(componentStructName, componentTypeStructName)
    val componentType = componentStructType(component)

    val methods = component.allCommandsMethods ++ component.allStatusMessageMethods ++ component.allMethods

    val externedCFile = (forwardFuncTableDecl.eol.eol ++ componentTypeForwardDecl.eol.eol ++
      Seq(componentType) ++ CSemicolon.eol ++ methods.flatMap(m => m.definition.eol)).externC
    hFile.writeIfNotEmptyWithComment((CEol +: appendPrologueEpilogue(imports ++ externedCFile))
      .protectDoubleInclude(dirPathForNs(component.namespace) + hFileName),
      s"Component ${component.name.asMangledString} interface")
    cFile.writeIfNotEmptyWithComment(CInclude(includePathForComponent(component)).eol.eol ++
      methods.flatMap(f => f.eol.eol), s"Component ${component.name.asMangledString} implementation")
  }

  private def structTypeFieldForCommand(structComponent: Component, component: Component,
                                        command: Command): CStructTypeDefField = {
    val methodName = command.cStructFieldName(structComponent, component)
    CStructTypeDefField(methodName, CFuncType(command.returnType.map(_.cType).getOrElse(voidType),
      command.cFuncParameterTypes(structComponent), methodName))
  }

  private def componentStructType(component: Component): CStructTypeDef = {
    val componentSelfPtrType = component.ptrType
    CStructTypeDef(Seq(CStructTypeDefField("data", CTypeApplication(component.componentDataTypeName).ptr)) ++
      component.allCommands.map { case ComponentCommand(c, command) =>
        structTypeFieldForCommand(component, c, command)
      } ++
      component.allParameters.map { case ComponentParameterField(c, f) =>
        val name = f.cStructFieldName(component, c)
        CStructTypeDefField(
          name, CFuncType(f.typeUnit.t.cType, Seq(componentSelfPtrType), name))
      } ++
      component.baseType.map(_.fields.map { f =>
        val name = f.mangledCName
        CStructTypeDefField(name, CFuncType(f.typeUnit.t.cType, Seq(componentSelfPtrType), name))
      }).getOrElse(Seq.empty) ++ component.commands.map(structTypeFieldForCommand(component, component, _)),
      Some(component.prefixedTypeName + structNamePostfix))
  }

  private def nsOrAliasCppSourceParts(ns: Namespace): Seq[String] =
    config.namespaceAliases.getOrElse(ns.fqn, Some(ns.fqn)).map(_.parts.map(_.asMangledString)).getOrElse(Seq.empty)

  private def dirPathForNs(ns: Namespace): String = nsOrAliasCppSourceParts(ns).mkString(io.File.separator)

  private def includePathForNsFileName(ns: Namespace, fileName: String): String =
    dirPathForNs(ns) + io.File.separator + fileName

  private def dirForNs(ns: Namespace): io.File = new io.File(config.outputDir, dirPathForNs(ns))

  private def relPathForType(t: DecodeType): String =
    dirPathForNs(t.namespace) + io.File.separator + t.fileName + headerExt

  private def includePathForComponent(comp: Component): String =
    includePathForNsFileName(comp.namespace, comp.prefixedTypeName + headerExt)

  private def prologuePath: String = config.prologue.path.getOrElse("photon_prologue.h")
  private def epiloguePath: String = config.epilogue.path.getOrElse("photon_epilogue.h")

  private def appendPrologueEpilogue(file: CAstElements): CAstElements = {
    (if (config.prologue.isActive)
      CInclude(prologuePath).eol.eol
    else
      CAstElements()) ++
      file ++
      (if (config.epilogue.isActive)
        Seq(CEol) ++ CInclude(epiloguePath).eol.eol
      else
        CAstElements())
  }

  private def generateTypeSeparateFiles(t: DecodeType, nsDir: io.File): Unit = if (!t.isNative) {
    val fileName = t.fileName
    val hFileName = fileName + headerExt
    val cFileName = fileName + sourcesExt
    val (hFile, cFile) = (new io.File(nsDir, hFileName), new io.File(nsDir, cFileName))
    val (h, c) = generateType(t, nsDir)
    if (h.nonEmpty)
      hFile.writeIfNotEmptyWithComment((CEol +: appendPrologueEpilogue(h).eol).protectDoubleInclude(relPathForType(t)),
        "Type header")
    else
      logger.debug(s"Omitting type ${t.name.asMangledString}")
    if (c.nonEmpty)
      cFile.writeIfNotEmptyWithComment(CInclude(relPathForType(t)).eol.eol ++ c, "Type implementation")
  }

  private def collectNsForType[T <: DecodeType](t: T, set: mutable.Set[Namespace]): Unit =
    t.collectNamespaces(set)

  def mapIfSmall[A <: B, B <: CAstElement](el: A, t: DecodeType, f: A => B): B = if (t.isSmall) f(el) else el

  def mapIfNotSmall[A <: B, B <: CAstElement](el: A, t: DecodeType, f: A => B): B = if (t.isSmall) el else f(el)

  def dotOrArrow(t: DecodeType, expr: CExpression, exprRight: CExpression): CExpression = t.isSmall match {
    case true => expr.dot(exprRight)
    case _ => expr -> exprRight
  }

  private def isAliasNameTheSame(t: AliasType): Boolean = t.cTypeName.equals(t.baseType.cTypeName)

  private def tryCall(methodName: String, exprs: CExpression*): CFuncCall = methodName.call(exprs: _*)._try

  implicit class ArrayTypeHelper(val t: ArrayType) {

    def serializeCodeForArrayElements(src: CExpression): CAstElements = {
      val baseType = t.baseType
      val dataExpr = dotOrArrow(t, src, dataVar)(i.v)
      val sizeExpr = dotOrArrow(t, src, size.v)
      Seq(CIndent, CForStatement(Seq(i.v.define(i.t, Some(CIntLiteral(0))), CComma, size.v.assign(sizeExpr)),
        Seq(CLess(i.v, size.v)), Seq(CIncBefore(i.v)),
        Seq(baseType.serializeCallCode(mapIfNotSmall(dataExpr, baseType, (expr: CExpression) => expr.ref)).line)), CEol)
    }

    def deserializeCodeForArrayElements(dest: CExpression): CAstElements = {
      val baseType = t.baseType
      val dataExpr = dest -> dataVar(i.v)
      Seq(CIndent, CForStatement(Seq(i.v.define(i.t, Some(CIntLiteral(0))), CComma, size.v.assign(dest -> size.v)),
        Seq(CLess(i.v, size.v)), Seq(CIncBefore(i.v)), Seq(baseType.deserializeCallCode(dataExpr.ref).line)), CEol)
    }
  }

  implicit class NativeTypeHelper(val t: NativeType) {

    private val berFqn = Fqn.newFromSource("decode.ber")

    def isBerType: Boolean = t.fqn.equals(berFqn)
  }

  implicit class DecodeTypeHelper(val t: DecodeType) {

    import DecodeTypeHelper._

    private val orFqn = Fqn.newFromSource("decode.or")
    private val optionalFqn = Fqn.newFromSource("decode.optional")

    def cMethodReturnType: CType = if (t.isSmall) t.cType else t.cType.ptr.const

    def cMethodReturnParameters: Seq[CFuncParam] = Seq.empty //if (t.isSmall) Seq.empty else Seq(CFuncParam("result", t.cType.ptr))

    def cType: CType = CTypeApplication(t.prefixedCTypeName)

    def isNative = t match {
      case _: NativeType => true
      case _ => false
    }

    def isOrType: Boolean = t.fqn.equals(orFqn)

    def isOptionalType: Boolean = t.fqn.equals(optionalFqn)

    def fileName: String = t.prefixedCTypeName

    def prefixedCTypeName: String = "PhotonGt" + cTypeName

    def isBasedOnEnum: Boolean = t match {
      case _: EnumType => true
      case _: ArrayType => false
      case t: HasBaseType => t.baseType.isBasedOnEnum
      case _ => false
    }

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
      case named => lowerUnderscoreToUpperCamel(named.name.asMangledString)
    }

    def isGeneratable: Boolean = t match {
      case _ if isNative => false
      case t: GenericType => false
      case _ => true
    }

    def byteSize: Int = t match {
      case t: NativeType if t.isPrimitive => (t.primitiveTypeInfo.bitLength / 8).toInt
      case t: GenericTypeSpecialized => t.genericType match {
        case optional if optional.isOptionalType => 1 + t.genericTypeArguments.head.getOrElse {
          sys.error("wtf")
        }.byteSize
        case or if or.isOrType => 1 + t.genericTypeArguments.map(_.map(_.byteSize).getOrElse(0)).max
        case _ => sys.error(s"not implemented for $t")
      }
      case t: NativeType => t match {
        case t if t.isBerType => BER_BYTE_SIZE
        case _ => sys.error(s"not implemented for $t")
      }
      case t: ArrayType => t.size.max match {
        case 0 => BER_BYTE_SIZE + PTR_SIZE
        case _ => (t.size.max * t.baseType.byteSize).toInt
      }
      case t: StructType => t.fields.map(_.typeUnit.t.byteSize).sum
      case t: HasBaseType => t.baseType.byteSize
      case _ => sys.error(s"not implemented for $t")
    }

    def isSmall: Boolean = t.byteSize <= 16

    def typeWithDependentTypes: immutable.Set[DecodeType] =
      (t match {
        case t: StructType => t.fields.flatMap(_.typeUnit.t.typeWithDependentTypes).toSet
        case t: HasBaseType => t.baseType.typeWithDependentTypes
        case t: GenericTypeSpecialized =>
          t.genericTypeArguments.flatMap(_.map(_.typeWithDependentTypes).getOrElse(Set.empty)).toSet ++
            t.genericType.t.typeWithDependentTypes
        case _: NativeType | _: GenericType => Set.empty[DecodeType]
        case _ => sys.error(s"not implemented for $t")
      }) + t

    def methodName(name: String): String = t.prefixedCTypeName.methodName(name)

    private val berSizeOf = "sizeof".call(berType)

    def abstractMinSizeExpr: Option[CExpression] = t match {
      case t: NativeType => Some("sizeof".call(t.cType))
      case t: AliasType => t.baseType.abstractMinSizeExpr
      case t: SubType => t.baseType.abstractMinSizeExpr
      case t: StructType => t.fields.map { f => f.typeUnit.t.abstractMinSizeExpr }.foldLeft[Option[CExpression]](None) {
        (l: Option[CExpression], r: Option[CExpression]) =>
          l.map { lExpr => r.map { rExpr => CPlus(lExpr, rExpr) }.getOrElse(lExpr) }.orElse(r)
      }
      case t: ArrayType =>
        t.baseType.abstractMinSizeExpr.map { rExpr => CPlus(berSizeOf, rExpr) }.orElse(Some(berSizeOf))
      case _ => sys.error(s"not implemented for $t")
    }

    def concreteMinSizeExpr(src: CExpression): Option[CExpression] = t match {
      case t: StructType => t.fields.map { f => f.typeUnit.t.concreteMinSizeExpr(src.dot(f.cName._var)) }.foldLeft[Option[CExpression]](None) {
        (l: Option[CExpression], r: Option[CExpression]) =>
          l.map { lExpr => r.map { rExpr => CPlus(lExpr, rExpr) }.getOrElse(lExpr) }.orElse(r)
      }
      case t: ArrayType =>
        t.baseType.abstractMinSizeExpr.map { rExpr => CMul(src.dot(size.v), rExpr) }.orElse(None)
      case _: SubType | _: AliasType | _: GenericTypeSpecialized => None // todo: yes you can
      case t: EnumType => t.baseType.concreteMinSizeExpr(src)
      case _ => abstractMinSizeExpr
    }

    private def writerSizeCheckCode(src: CExpression) = concreteMinSizeExpr(
      mapIfNotSmall(src, t, (src: CExpression) => CParens(src.deref))).map { sizeExpr =>
      Seq(CIndent, CIf(CLess("PhotonWriter_WritableSize".call(writer.v), sizeExpr),
        Seq(CEol, CReturn("PhotonResult_NotEnoughSpace"._var).line)))
    }.getOrElse(Seq.empty)

    private def readerSizeCheckCode(dest: CExpression) = concreteMinSizeExpr(CParens(dest.deref)).map { sizeExpr =>
      Seq(CIndent, CIf(CLess("PhotonReader_ReadableSize".call(reader.v), sizeExpr),
        Seq(CEol, CReturn("PhotonResult_NotEnoughData"._var).line)))
    }.getOrElse(Seq.empty)

    def serializeMethodName: String = methodName(typeSerializeMethodName)

    def deserializeMethodName: String = methodName(typeDeserializeMethodName)

    private def trySerializeFuncCall(src: CExpression): CFuncCall = serializeFuncCall(src)._try

    private def tryDeserializeFuncCall(dest: CExpression): CFuncCall = deserializeFuncCall(dest)._try

    private def serializeFuncCall(src: CExpression): CFuncCall = methodName(typeSerializeMethodName).call(src, writer.v)

    private def deserializeFuncCall(dest: CExpression): CFuncCall = methodName(typeDeserializeMethodName).call(dest, reader.v)

    private def callCodeForBer(methodNamePart: String, exprs: CExpression*): CExpression =
      photonBerTypeName.methodName(methodNamePart).call(exprs: _*)

    def serializeBerCallCode(src: CExpression): CExpression = callCodeForBer(typeSerializeMethodName, src, writer.v)

    def deserializeBerCallCode(dest: CExpression): CExpression = callCodeForBer(typeDeserializeMethodName, dest, reader.v)

    def serializeCallCode(src: CExpression): CExpression = t match {
      case _: ArrayType | _: StructType => serializeFuncCall(src)
      case t: NativeType if t.isPrimitive =>
        callCodeForPrimitiveType(t.primitiveTypeInfo, src, photonWriterTypeName, "Write", writer.v, src)
      case t: NativeType => t match {
        case t if t.isBerType => callCodeForBer(typeSerializeMethodName, src, writer.v)
        case _ => sys.error(s"not implemented for $t")
      }
      case t: HasBaseType =>
        t.baseType.serializeCallCode(src)
      case t =>
        serializeFuncCall(src)
    }

    def deserializeCallCode(dest: CExpression): CExpression = t match {
      case _: ArrayType | _: StructType => deserializeFuncCall(dest)
      case t: NativeType if t.isPrimitive =>
        CAssign(CDeref(dest), callCodeForPrimitiveType(t.primitiveTypeInfo, dest, photonReaderTypeName, "Read", reader.v))
      case t: NativeType => t match {
        case t if t.isBerType => callCodeForBer(typeDeserializeMethodName, dest, reader.v)
        case _ => sys.error(s"not implemented for $t")
      }
      case t: HasBaseType =>
        val baseType = t.baseType
        baseType.deserializeCallCode(dest.cast(baseType.cType.ptr))
      case _ => sys.error(s"not implemented for $t")
    }

    private val flagVar: CVar = "flag"._var
    private val valueVar: CVar = "value"._var
    private val tagVar: CVar = "tag"._var

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

    def serializeCode: CAstElements = serializeCode(selfVar)

    def serializeCode(src: CExpression): CAstElements = t match {
      case t: StructType => writerSizeCheckCode(src) ++ t.fields.flatMap { f =>
        val fType = f.typeUnit.t
        val fVar = f.cName._var
        Seq(fType.serializeCallCode((if (t.isSmall) src.dot(fVar) else src -> fVar).refIfNotSmall(fType)).line)
      }
      case t: ArrayType =>
        writerSizeCheckCode(src) ++ src.serializeCodeForArraySize(t) ++ t.serializeCodeForArrayElements(src)
      case t: AliasType => Seq(t.baseType.serializeCallCode(src).line)
      case t: SubType => Seq(t.baseType.serializeCallCode(src).line)
      case t: EnumType => Seq(t.baseType.serializeCallCode(src).line)
      case t: NativeType if t.isPrimitive => Seq(t.serializeCallCode(src).line)
      case t: NativeType => Seq(t.serializeCallCode(src).line)
      case t: GenericTypeSpecialized => serializeGenericTypeSpecializedCode(t, src)
      case _ => sys.error(s"not implemented for $t")
    }

    def deserializeCode: CAstElements = deserializeCode(selfVar)

    def deserializeCode(dest: CExpression): CAstElements = t match {
      case t: StructType => t.fields.flatMap { f =>
        Seq(f.typeUnit.t.deserializeCallCode((dest -> f.cName._var).ref).line)
      }
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

  implicit class RichComponent(val component: Component) {
    def allCommands: Seq[WithComponent[Command]] =
      allSubComponents.toSeq.flatMap(sc => sc.commands.map(ComponentCommand(sc, _)))

    def allParameters: Seq[ComponentParameterField] =
      allSubComponents.toSeq.flatMap(sc => sc.baseType.map(_.fields.map(ComponentParameterField(sc, _)))
        .getOrElse(Seq.empty))

    def typeName: String = component.name.asMangledString

    def executeCommandMethodNamePart: String = "ExecuteCommand"

    def functionForCommandMethodName: String = component.prefixedTypeName.methodName("FunctionForCommand")

    def executeCommandMethodName(rootComponent: Component): String =
      if (component == rootComponent)
        component.prefixedTypeName.methodName(executeCommandMethodNamePart)
      else
        rootComponent.prefixedTypeName.methodName(component.cName + executeCommandMethodNamePart)

    def readExecuteCommandMethodNamePart: String = "ReadExecuteCommand"

    def readExecuteCommandMethodName(rootComponent: Component): String =
      if (component == rootComponent)
        component.prefixedTypeName.methodName(readExecuteCommandMethodNamePart)
      else
        rootComponent.prefixedTypeName.methodName(component.cName + readExecuteCommandMethodNamePart)

    def writeStatusMessageMethodName(rootComponent: Component): String =
      if (component == rootComponent)
        component.prefixedTypeName.methodName("WriteStatusMessage")
      else
        rootComponent.prefixedTypeName.methodName(component.cName + "WriteMessage")

    def isStatusMessageMethodName: String = component.prefixedTypeName.methodName("IsStatusMessage")

    def isEventAllowedMethodName: String = component.prefixedTypeName.methodName("IsEventAllowed")

    def prefixedTypeName: String = typePrefix + typeName

    def componentDataTypeName: String = prefixedTypeName + "Data"

    def ptrType: CPtrType = CTypeApplication(prefixedTypeName).ptr

    def functionTableTypeName: String = prefixedTypeName + "UserFunctionTable"

    def guidDefineName: String = upperCamelCaseToUpperUnderscore(component.prefixedTypeName) + "_GUID"

    def idDefineName: String = upperCamelCaseToUpperUnderscore(component.prefixedTypeName) + "_ID"

    def allSubComponents: immutable.Set[Component] =
      component.subComponents.flatMap { ref =>
        val c = ref.component
        c.allSubComponents + c
      }.toSet

    def collectNamespaces(nsSet: mutable.HashSet[Namespace]) {
      component.subComponents.foreach(_.component.collectNamespaces(nsSet))
      collectNsForTypes(nsSet)
    }

    def collectNsForTypes(set: mutable.Set[Namespace]) {
      for (baseType <- component.baseType)
        collectNsForType(baseType, set)
      component.commands.foreach { cmd =>
        cmd.parameters.foreach(arg => collectNsForType(arg.paramType, set))
        for (returnType <- cmd.returnType)
          collectNsForType(returnType, set)
      }
    }

    def writeStatusMessageMethod: CFuncImpl = writeStatusMessageMethod(component)

    def writeStatusMessageMethod(rootComponent: Component): CFuncImpl = {
      CFuncImpl(CFuncDef(component.writeStatusMessageMethodName(rootComponent), resultType,
        Seq(writer.param, messageId.param)),
        Seq(messageId.v.serializeBer._try.line, CIndent, CSwitch(messageId.v,
          casesForMap(component.allStatusMessagesById, { (message: TmMessage, c: Component) => message match {
            case message: StatusMessage =>
              Some(CStatements(CReturn(message.fullMethodName(rootComponent, c).call(writer.v))))
            case _ => None
          }
          }),
          default = CStatements(CReturn(invalidMessageId))), CEol))
    }

    def executeCommandMethod: CFuncImpl = executeCommandMethod(component)

    def executeCommandMethod(rootComponent: Component): CFuncImpl = {
      CFuncImpl(CFuncDef(component.executeCommandMethodName(rootComponent), resultType,
        Seq(reader.param, writer.param, commandId.param)),
        Seq(CIndent, CSwitch(commandId.v, casesForMap(component.allCommandsById,
          (command: Command, c: Component) =>
            Some(CStatements(CReturn(command.executeMethodName(rootComponent, c).call(reader.v, writer.v))))),
          default = CStatements(CReturn(invalidCommandId))), CEol))
    }

    def readExecuteCommandMethod: CFuncImpl = readExecuteCommandMethod(component)

    def readExecuteCommandMethod(rootComponent: Component): CFuncImpl = {
      CFuncImpl(CFuncDef(component.readExecuteCommandMethodName(rootComponent), resultType,
        Seq(reader.param, writer.param)),
        CStatements(commandId.v.define(commandId.t),
          tryCall(photonBerTypeName.methodName(typeDeserializeMethodName), commandId.v.ref, reader.v),
          CReturn(CFuncCall(component.executeCommandMethodName(rootComponent), reader.v, writer.v, commandId.v))))
    }

    def executeCommandForComponentMethodNamePart: String = "ExecuteCommandForComponent"

    def executeCommandForComponentMethodName(rootComponent: Component): String =
      if (component == rootComponent)
        component.prefixedTypeName.methodName(executeCommandForComponentMethodNamePart)
      else
        rootComponent.prefixedTypeName.methodName(executeCommandForComponentMethodNamePart + component.cName)

    def executeCommandForComponentMethod(rootComponent: Component): CFuncImpl = {
      CFuncImpl(CFuncDef(component.executeCommandForComponentMethodName(rootComponent), resultType,
        Seq(reader.param, writer.param, commandId.param)),
        Seq(CIndent, CSwitch(commandId.v, component.allComponentsById.toSeq.sortBy(_._1).map { case (id, c) =>
          CCase(CIntLiteral(id), CStatements(CReturn(c.readExecuteCommandMethodName(rootComponent)
            .call(reader.v, writer.v))))
        }, default = CStatements(CReturn(invalidCommandId))), CEol))
    }

    def executeCommandForComponentMethod: CFuncImpl = {
      CFuncImpl(CFuncDef(component.executeCommandForComponentMethodName(component), resultType,
        Seq(reader.param, writer.param, componentId.param, commandId.param)),
        Seq(CIndent, CSwitch(componentId.v, component.allComponentsById.toSeq.sortBy(_._1).map { case (id, c) =>
          CCase(CIntLiteral(id), CStatements(CReturn(
            (if (c == component)
              c.executeCommandMethodName(component)
            else
              c.executeCommandForComponentMethodName(component))
              .call(reader.v, writer.v, commandId.v))))
        }, default = CStatements(CReturn(invalidComponentId))), CEol))
    }

    def allMethods: Seq[CFuncImpl] = component.allCommandsMethods ++ component.allStatusMessageMethods ++
      component.allEventMessageMethods ++
      Seq(component.executeCommandMethod, component.readExecuteCommandMethod, component.writeStatusMessageMethod,
        component.executeCommandForComponentMethod) ++
      component.allSubComponents.map(_.executeCommandForComponentMethod(component))

    def parameterMethodName(parameter: MessageParameter, rootComponent: Component): String =
      rootComponent.prefixedTypeName.methodName(parameter.ref(component).structField
        .getOrElse {
          sys.error("not implemented")
        }.cStructFieldName(rootComponent, component))

    def allEventMessageMethods: Seq[CFuncImpl] = {
      allEventMessagesById.toSeq.sortBy(_._1).flatMap {
        case (id, ComponentEventMessage(c, eventMessage)) =>
          val eventVar = "event"._var
          Some(CFuncImpl(CFuncDef(eventMessage.fullMethodName(component, c), resultType,
            Seq(writer.param, CFuncParam("event", eventMessage.baseType.cType)) ++ eventMessage.fields.flatMap {
              case Right(e) =>
                val t = e.paramType
                Seq(CFuncParam(e.cName, mapIfNotSmall(t.cType, t, (t: CType) => t.ptr.const)))
              case _ => Seq.empty
            }),
            CAstElements(CIndent, CIf(CEq(CIntLiteral(0),
              component.isEventAllowedMethodName.call(CIntLiteral(id), CTypeCast(eventVar, berType))),
              CAstElements(CEol, CIndent, CReturn(eventIsDenied), CSemicolon, CEol)),
              eventMessage.baseType.serializeCallCode(eventVar).line) ++
              eventMessage.fields.flatMap {
                case Left(p) =>
                  val v = p.varName._var
                  val parameterRef = p.ref(c)
                  val structField = parameterRef.structField.getOrElse {
                    sys.error("not implemented")
                  }
                  val t = structField.typeUnit.t
                  if (parameterRef.structField.isDefined) {
                    val t = parameterRef.t
                    if (parameterRef.path.isEmpty)
                      CStatements(t.serializeCallCode(c.parameterMethodName(p, component).call()))
                    else
                      sys.error("not implemented")
                  } else {
                    sys.error("not implemented")
                  }
                case Right(p) =>
                  CStatements(p.paramType.serializeCallCode(p.cName._var))
              } :+ CReturn(resultOk).line
          ))
      }
    }

    def allStatusMessageMethods: Seq[CFuncImpl] = {
      allStatusMessagesById.toSeq.sortBy(_._1).map(_._2).flatMap {
        case ComponentStatusMessage(c, statusMessage) =>
          Some(CFuncImpl(CFuncDef(statusMessage.fullMethodName(component, c), resultType,
            Seq(writer.param)),
            statusMessage.parameters.flatMap { p =>
              val v = p.varName._var
              val parameterRef = p.ref(c)
              if (parameterRef.structField.isDefined) {
                val t = parameterRef.t
                if (parameterRef.path.isEmpty)
                  Seq(t.serializeCallCode(c.parameterMethodName(p, component).call()).line)
                else
                  sys.error("not implemented")
              } else {
                sys.error("not implemented")
              }
            } :+ CReturn(resultOk).line))
      }
    }

    def allCommandDefines: CAstElements = allCommandDefines(component)

    def allCommandDefines(rootComponent: Component): CAstElements =
      component.commandDefines(rootComponent) ++ component.allSubComponents.toSeq.flatMap(_.commandDefines(component))

    def commandDefines(rootComponent: Component): CAstElements = {
      val defineName: String = upperCamelCaseToUpperUnderscore(rootComponent.prefixedTypeName +
        (if (rootComponent == component) "" else component.cName) + "CommandIds")
      val commandById = component.allCommandsById
      CDefine(defineName + "_LEN", commandById.size.toString).eol ++
        CDefine(defineName, "{" + commandById.toSeq.map(_._1).sorted.mkString(", ") + "}").eol
    }

    def componentDefines: CAstElements = {
      Seq(CDefine("PHOTON_COMPONENTS_SIZE", allComponentsById.size.toString), CEol,
        CDefine("PHOTON_COMPONENT_IDS", '{' + allComponentsById.keys.toSeq.sorted.mkString(", ") + '}'), CEol) ++
        allComponentsById.flatMap { case (id, c) =>
          val guidDefineName = c.guidDefineName
          Seq(CDefine(guidDefineName, '"' + c.fqn.asMangledString + '"'), CEol,
            CDefine("PHOTON_COMPONENT_" + id + "_GUID", guidDefineName), CEol,
            CDefine(c.idDefineName, id.toString), CEol)
        } ++
        Seq(CDefine("PHOTON_COMPONENT_GUIDS", '{' + allComponentsById.toSeq.sortBy(_._1).map(_._2)
          .map(_.guidDefineName).mkString(", ") + '}'), CEol)
    }

    def allMessageDefines: CAstElements = allMessageDefines(component)

    def allMessageDefines(rootComponent: Component): CAstElements =
      component.messageDefines(rootComponent) ++ component.allSubComponents.toSeq.flatMap(_.messageDefines(component))

    def messageDefines(rootComponent: Component): CAstElements = {
      val prefix = upperCamelCaseToUpperUnderscore(rootComponent.prefixedTypeName +
        (if (rootComponent == component) "" else component.cName))
      val eventIdsDefineName = prefix + "_EVENT_MESSAGE_IDS"
      val statusIdsDefineName = prefix + "_STATUS_MESSAGE_IDS"
      val statusMessageIdPrioritiesDefineName = prefix + "_STATUS_MESSAGE_ID_PRIORITIES"
      val statusPrioritiesDefineName = prefix + "_STATUS_MESSAGE_PRIORITIES"
      val statusMessageById = component.allStatusMessagesById
      val statusMessagesSortedById = statusMessageById.toSeq.sortBy(_._1)
      val eventMessageById = component.allEventMessagesById
      val eventMessagesSortedById = eventMessageById.toSeq.sortBy(_._1)
      CDefine(eventIdsDefineName + "_SIZE", eventMessageById.size.toString).eol ++
        CDefine(eventIdsDefineName, "{" + eventMessagesSortedById.map(_._1).mkString(", ") + "}").eol ++
        CDefine(statusIdsDefineName + "_SIZE", statusMessagesSortedById.size.toString).eol ++
        CDefine(statusIdsDefineName, '{' + statusMessagesSortedById.map(_._1.toString).mkString(", ") + '}').eol ++
        CDefine(statusPrioritiesDefineName, "{" + statusMessagesSortedById.map(_._2._2.priority.getOrElse(0)).mkString(", ") + "}").eol ++
        CDefine(statusMessageIdPrioritiesDefineName, "{\\\n" + statusMessagesSortedById.map {
          case (id, ComponentStatusMessage(c, m)) => s"  {$id, ${m.priority.getOrElse(0)}}"
          case _ => sys.error("assertion error")
        }.mkString("\\\n") + "\\\n}").eol
    }

    def parameterMethodImplDefs: Seq[CFuncDef] = {
      val componentTypeName = component.prefixedTypeName
      component.allParameters.map { case ComponentParameterField(c, f) =>
        val fType = f.typeUnit.t
        CFuncDef(componentTypeName.methodName(f, component, c), fType.cMethodReturnType, fType.cMethodReturnParameters)
      }
    }

    def commandMethodImplDefs: Seq[CFuncDef] = {
      val componentTypeName = component.prefixedTypeName
      component.allCommands.map { case ComponentCommand(c, command) =>
        CFuncDef(componentTypeName.methodName(command, component, c),
          command.returnType.map(_.cMethodReturnType).getOrElse(voidType),
          command.parameters.map(p => {
            val t = p.paramType
            CFuncParam(p.cName, mapIfNotSmall(t.cType, t, (ct: CType) => ct.ptr.const))
          }) ++ command.returnType.map(_.cMethodReturnParameters).getOrElse(Seq.empty))
      }
    }

    def allSubComponentsMethods: Seq[CFuncImpl] = {
      component.allSubComponents.toSeq.flatMap { subComponent =>
        Seq(subComponent.executeCommandMethod(component), subComponent.readExecuteCommandMethod(component),
          subComponent.writeStatusMessageMethod(component))
      }
    }

    def allCommandsMethods: Seq[CFuncImpl] = {
      val componentTypeName = component.prefixedTypeName
      val parameters = Seq(reader.param, writer.param)
      component.allCommandsById.toSeq.sortBy(_._1).map(_._2).map { case ComponentCommand(subComponent, command) =>
        val methodNamePart = command.executeMethodNamePart(component, subComponent)
        val vars = command.parameters.map { p => CVar(p.mangledCName) }
        val varInits = vars.zip(command.parameters).flatMap { case (v, parameter) =>
          val paramType = parameter.paramType
          CStatements(v.define(paramType.cType), paramType.deserializeCallCode(v.ref)._try)
        }
        val cmdReturnType = command.returnType
        //val cmdCReturnType = cmdReturnType.map(_.cType)
        val funcCall = command.methodName(component, subComponent).call(
          (for ((v, t) <- vars.zip(command.parameters.map(_.paramType))) yield v.refIfNotSmall(t)): _*)
        CFuncImpl(CFuncDef(componentTypeName.methodName(methodNamePart), resultType, parameters),
          varInits ++ cmdReturnType.map {
            case n: NativeType if n.isPrimitive => CStatements(n.serializeCallCode(funcCall), CReturn(resultOk))
            case rt => CStatements(CReturn(rt.serializeCallCode(funcCall)))
          }.getOrElse(CStatements(funcCall, CReturn(resultOk))))
      }
    }

    def allTypes: immutable.Set[DecodeType] =
      (component.commands.flatMap(cmd => cmd.returnType.map(_.typeWithDependentTypes).getOrElse(Seq.empty) ++
        cmd.parameters.flatMap(_.paramType.typeWithDependentTypes)) ++
        component.eventMessages.map(_.baseType) ++
        component.baseType.map(_.fields.flatMap(_.typeUnit.t.typeWithDependentTypes)).getOrElse(Seq.empty)).toSet ++
        component.allSubComponents.flatMap(_.allTypes)

    // todo: optimize: memoize
    private def makeMapById[T <: HasOptionId](seq: Seq[T], subSeq: Component => Seq[T])
    : immutable.HashMap[Int, WithComponent[T]] = {
      var nextId = 0
      val mapById = mutable.HashMap.empty[Int, WithComponent[T]]
      // fixme: remove Option.get
      seq.filter(_.id.isDefined).foreach(el => assert(mapById.put(el.id.get, WithComponent[T](component, el)).isEmpty))
      seq.filter(_.id.isEmpty).foreach { el =>
        // todo: optimize: too many contain checks
        while (mapById.contains(nextId))
          nextId += 1
        assert(mapById.put(el.id.getOrElse {
          nextId += 1
          nextId - 1
        }, WithComponent[T](component, el)).isEmpty)
      }
      component.allSubComponents.toSeq.sortBy(_.fqn.asMangledString).filterNot(_ == component).foreach(subComponent =>
        subSeq(subComponent).foreach { el =>
          assert(mapById.put(nextId, WithComponent[T](subComponent, el)).isEmpty)
          nextId += 1
        })
      immutable.HashMap(mapById.toSeq: _*)
    }

    def allCommandsById: immutable.HashMap[Int, WithComponent[Command]] =
      makeMapById(component.commands, _.commands)

    def allStatusMessagesById: immutable.HashMap[Int, WithComponent[StatusMessage]] =
      makeMapById(component.statusMessages, _.statusMessages)

    def allEventMessagesById: immutable.HashMap[Int, WithComponent[EventMessage]] =
      makeMapById(component.eventMessages, _.eventMessages)

    def allComponentsById: immutable.HashMap[Int, Component] = {
      val map = mutable.HashMap.empty[Int, Component]
      var nextId = 0
      val components = component +: component.allSubComponents.toSeq
      val (withId, withoutId) = (components.filter(_.id.isDefined), components.filter(_.id.isEmpty))
      withId.foreach { c => assert(map.put(c.id.getOrElse(sys.error("wtf")), c).isEmpty) }
      map ++= withoutId.map { c =>
        while (map.contains(nextId))
          nextId += 1
        nextId += 1
        (nextId - 1, c)
      }
      immutable.HashMap(map.toSeq: _*)
    }
  }

  implicit class RichCommand(val command: Command) {
    def cFuncParameterTypes(component: Component): Seq[CType] = {
      component.ptrType +: command.parameters.map(p => {
        val t = p.paramType
        mapIfNotSmall(t.cType, t, (ct: CType) => ct.ptr)
      })
    }
  }

  implicit class RichMessage(val message: TmMessage) {
    def fullMethodName(rootComponent: Component, component: Component): String =
      rootComponent.prefixedTypeName.methodName("Write" + message.methodNamePart(rootComponent, component).capitalize)
  }

  implicit class HasNameHelper(val named: HasName) {

    def executeMethodNamePart(rootComponent: Component, component: Component): String =
      "Execute" + methodNamePart(rootComponent, component).capitalize

    def executeMethodName(rootComponent: Component, component: Component): String =
      rootComponent.prefixedTypeName.methodName(executeMethodNamePart(rootComponent, component))

    def methodNamePart(rootComponent: Component, component: Component): String =
      upperCamelCaseToLowerCamelCase((if (rootComponent == component) "" else component.typeName) +
        cName.capitalize)

    def methodName(rootComponent: Component, component: Component): String =
      rootComponent.prefixedTypeName.methodName(methodNamePart(rootComponent, component))

    def cName: String = named.name.asMangledString

    def fileName: String = named.name.asMangledString

    def cTypeName: String = named.name.asMangledString

    def mangledCName: String = {
      var methodName = cName
      if (keywords.contains(methodName))
        methodName = "_" + methodName
      methodName
    }

    def cStructFieldName(structComponent: Component, component: Component): String =
      upperCamelCaseToLowerCamelCase((if (structComponent == component) "" else component.cName) +
        named.mangledCName.capitalize)
  }

  implicit class RichConstExpr(val c: ConstExpr) {
    def toInt: Int = c match {
      case i: IntLiteral => i.value
      case _ => sys.error("not implemented")
    }
  }

  implicit class RichString(val str: String) {
    def _var: CVar = CVar(str)

    def call(exprs: CExpression*) = CFuncCall(str, exprs: _*)

    def tryCall(exprs: CExpression*) = str.call(exprs: _*)._try

    def methodName(name: String): String = str + "_" + name.capitalize

    def initMethodName: String = methodName(typeInitMethodName)

    def methodName(command: Command, rootComponent: Component, component: Component): String =
      methodName(command.methodNamePart(rootComponent, component))

    def methodName(f: StructField, rootComponent: Component, component: Component): String =
      methodName(f.methodNamePart(rootComponent, component))

    def comment: CAstElements = Seq(CEol, CComment(str), CEol)
  }

  private object DecodeTypeHelper {
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
  }

  implicit class RichExpression(val expr: CExpression) {
    def _try: CFuncCall = tryMacroName.call(expr)

    def ->(expr2: CExpression): CArrow = CArrow(expr, expr2)

    def apply(indexExpr: CExpression): CIndex = CIndex(expr, indexExpr)

    def ref: CExpression = expr match {
      case expr: CDeref => expr.expr
      case _ => CRef(expr)
    }

    def refIfNotSmall(t: DecodeType): CExpression = if (t.isSmall) expr else expr.ref

    def derefIfSmall(t: DecodeType): CExpression = if (t.isSmall) expr.deref else expr

    def assign(right: CExpression) = CAssign(expr, right)

    def dot(right: CExpression) = CDot(expr, right)

    def deref = CDeref(expr)

    def cast(cType: CType): CTypeCast = CTypeCast(expr, cType)

    def serializeCodeForArraySize(t: ArrayType): CAstElements = {
      val sizeExpr = t.isSmall match {
        case false => expr -> size.v
        case _ => expr.dot(size.v)
      }
      CStatements(photonBerTypeName.methodName(typeSerializeMethodName).call(
        Seq(sizeExpr, writer.v): _*)._try)
    }

    def deserializeCodeForArraySize(t: ArrayType): CAstElements = {
      val sizeExpr = expr -> size.v
      CStatements(photonBerTypeName.methodName(typeDeserializeMethodName).call(
        Seq(sizeExpr.ref, reader.v): _*)._try)
    }
  }

  implicit class RichVar(val v: CVar) {
    def define(t: CType, init: Option[CExpression] = None, static: Boolean = false) = CVarDef(v.name, t, init, static)

    def serializeBer: CFuncCall = photonBerTypeName.methodName(typeSerializeMethodName).call(v, writer.v)
  }

  implicit class RichCAstElements(val els: CAstElements) {

    def protectDoubleInclude(filePath: String): CAstElements = {
      val uniqueName = "__" + filePath.split(io.File.separatorChar).map(p =>
        upperCamelCaseToUpperUnderscore(p).replaceAll("\\.", "_")).mkString("_") + "__"
      "DO NOT EDIT! FILE IS AUTO GENERATED".comment.eol ++ Seq(CIfNDef(uniqueName), CEol, CDefine(uniqueName)) ++ els :+ CEndIf
    }

    def externC: CAstElements =
      Seq(CIfDef(cppDefine), CEol, CPlainText("extern \"C\" {"), CEol, CEndIf, CEol, CEol) ++ els ++
        Seq(CEol, CEol, CIfDef(cppDefine), CEol, CPlainText("}"), CEol, CEndIf)

    def eol: CAstElements = els :+ CEol
  }

}

object CSourceGenerator {

  class WithComponent[T](val component: Component, val _2: T)

  object WithComponent {
    def apply[T](component: Component, _2: T) = new WithComponent[T](component, _2)

    def unapply[T](o: WithComponent[T]): Option[(Component, T)] = Some(o.component, o._2)
  }

  object ComponentCommand {
    def apply(component: Component, command: Command) = WithComponent[Command](component, command)

    def unapply(o: WithComponent[Command]) = WithComponent.unapply(o)
  }

  object ComponentEventMessage {
    def apply(component: Component, message: EventMessage) = WithComponent[EventMessage](component, message)

    def unapply(o: WithComponent[EventMessage]) = WithComponent.unapply(o)
  }

  object ComponentStatusMessage {
    def apply(component: Component, message: StatusMessage) = WithComponent[StatusMessage](component, message)

    def unapply(o: WithComponent[StatusMessage]) = WithComponent.unapply(o)
  }

  implicit class ParameterHelper(val parameter: MessageParameter) {
    def varName: String = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL,
      parameter.path.toString.replaceAll("[\\.\\[\\]]", "_").replaceAll("__", "_"))
  }

  implicit class RichAstElement(val el: CAstElement) {
    def line: CStatementLine = CStatementLine(el)

    def eol: CAstElements = Seq(el, CEol)
  }

  implicit class RichCType(val ct: CType) {
    def const: CConstType = CConstType(ct)
  }

  implicit class FileHelper(val file: io.File) {
    def write(contents: CAstElements) {
      for (os <- managed(new OutputStreamWriter(new FileOutputStream(file)))) {
        contents.generate(CGenState(os))
      }
    }

    def write(source: Source): Unit = write(source.mkString)

    def write(contents: String): Unit = {
      for (os <- managed(new OutputStreamWriter(new FileOutputStream(file)))) {
        os.write(contents)
      }
    }

    def writeIfNotEmptyWithComment(contents: CAstElements, comment: String) {
      if (contents.nonEmpty)
        file.write(CComment(comment).eol ++ contents)
    }
  }

  private val PTR_SIZE = 4
  private val BER_BYTE_SIZE = 8

  private val headerExt = ".h"
  private val sourcesExt = ".c"
  private val structNamePostfix = "_s"
  private val cppDefine = "__cplusplus"
  private val typePrefix = "PhotonGc"

  private val tryMacroName = "PHOTON_TRY"
  private val typeInitMethodName = "Init"
  private val typeSerializeMethodName = "Serialize"
  private val typeDeserializeMethodName = "Deserialize"
  private val photonBerTypeName = "PhotonBer"
  private val photonWriterTypeName = "PhotonWriter"
  private val photonReaderTypeName = "PhotonReader"

  private val b8Type = CTypeApplication("PhotonGtB8")
  private val berType = CTypeApplication("PhotonBer")
  private val voidType = CTypeApplication("void")
  private val sizeTType = CTypeApplication("size_t")

  private val resultType = CTypeApplication("PhotonResult")
  private val resultOk = CVar(resultType.name + "_Ok")

  private val selfVar = CVar("self")
  private val dataVar = CVar("data")
  private val invalidMessageId = CVar("PhotonResult_InvalidMessageId")
  private val invalidCommandId = CVar("PhotonResult_InvalidCommandId")
  private val invalidComponentId = CVar("PhotonResult_InvalidComponentId")
  private val invalidValue = CVar("PhotonResult_InvalidValue")
  private val eventIsDenied = CVar("PhotonResult_EventIsDenied")

  private val tag = TypedVar("tag", berType)
  private val size = TypedVar("size", berType)
  private val i = TypedVar("i", berType)
  private val reader = TypedVar("reader", CTypeApplication("PhotonReader").ptr)
  private val writer = TypedVar("writer", CTypeApplication("PhotonWriter").ptr)
  private val componentId = TypedVar("componentId", berType)
  private val commandId = TypedVar("commandId", berType)
  private val messageId = TypedVar("messageId", berType)
  private val eventId = TypedVar("eventId", berType)

  private def upperCamelCaseToLowerCamelCase(str: String) = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, str)

  private def allComponentsSetForComponent(component: Component,
                                           componentsSet: immutable.HashSet[Component] = immutable.HashSet.empty)
  : immutable.HashSet[Component] = {
    componentsSet ++ Seq(component) ++ component.subComponents.flatMap(cr =>
      allComponentsSetForComponent(cr.component, componentsSet))
  }

  private def lowerUnderscoreToUpperCamel(str: String) = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, str)

  private def primitiveTypeToCTypeApplication(primitiveType: PrimitiveTypeInfo): CTypeApplication = {
    import TypeKind._
    (primitiveType.kind, primitiveType.bitLength) match {
      case (Bool, 8) => CUint8TType
      case (Bool, 16) => CUint16TType
      case (Bool, 32) => CUint32TType
      case (Bool, 64) => CUint64TType
      case (Float, 32) => CFloatType
      case (Float, 64) => CDoubleType
      case (Int, 8) => CInt8TType
      case (Int, 16) => CInt16TType
      case (Int, 32) => CInt32TType
      case (Int, 64) => CInt64TType
      case (Uint, 8) => CUint8TType
      case (Uint, 16) => CUint16TType
      case (Uint, 32) => CUint32TType
      case (Uint, 64) => CUint64TType
      case _ => sys.error("illegal bit length")
    }
  }

  private def upperCamelCaseToUpperUnderscore(s: String) = CaseFormat.UPPER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, s)

  private val keywords = Seq("return")

  private def casesForMap[T <: HasName](map: immutable.HashMap[Int, WithComponent[T]],
                                        apply: (T, Component) => Option[CAstElements]): Seq[CCase] =
    map.toSeq.sortBy(_._1).flatMap { case (id, WithComponent(c, _2)) => apply(_2, c).map(els => CCase(CIntLiteral(id), els)) }
}
