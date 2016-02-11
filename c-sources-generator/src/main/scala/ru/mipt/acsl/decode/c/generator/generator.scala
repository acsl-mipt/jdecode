package ru.mipt.acsl.decode.c.generator

import java.io
import java.io.{File, OutputStreamWriter, FileOutputStream}
import java.security.MessageDigest

import com.google.common.base.CaseFormat
import com.typesafe.scalalogging.LazyLogging
import ru.mipt.acsl.decode.model.domain._
import ru.mipt.acsl.decode.model.domain.impl.`type`.DecodeBerType
import ru.mipt.acsl.decode.model.domain.impl.`type`.DecodeOptionalType
import ru.mipt.acsl.decode.model.domain.impl.`type`.DecodeOrType
import ru.mipt.acsl.generation.Generator
import ru.mipt.acsl.generator.c.ast.implicits._
import ru.mipt.acsl.generator.c.ast._

import resource._

import scala.collection.immutable.HashMap
import scala.collection.mutable
import scala.collection.immutable
import scala.util.Random

case class CGeneratorConfiguration(outputDir: io.File, registry: DecodeRegistry, rootComponentFqn: String,
                                   namespaceAliases: Map[DecodeFqn, Option[DecodeFqn]] = Map.empty,
                                   prologueEpiloguePath: Option[String] = None, isSingleton: Boolean = false,
                                   arrayAsPointer: Boolean = true)

class CSourcesGenerator(val config: CGeneratorConfiguration) extends Generator[CGeneratorConfiguration] with LazyLogging {

  import CSourcesGenerator._

  override def getConfiguration: CGeneratorConfiguration = config

  override def generate(): Unit = {
    val component = config.registry.getComponent(config.rootComponentFqn).getOrElse(
      sys.error(s"component not found ${config.rootComponentFqn}"))
    generateRootComponent(component)
  }

  private def ensureDirForNsExists(ns: DecodeNamespace): io.File = {
    val dir = dirForNs(ns)
    if (!(dir.exists() || dir.mkdirs()))
      sys.error(s"Can't create directory ${dir.getAbsolutePath}")
    dir
  }

  private def generateNs(ns: DecodeNamespace): Unit = {
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
      case t: DecodePrimitiveType => (Seq(t.cTypeDef(t.cType)), Seq.empty)
      case t: DecodeNativeType => (Seq(t.cTypeDef(CVoidType.ptr)), Seq.empty)
      case t: DecodeSubType => (Seq(t.cTypeDef(t.baseType.obj.cType)), Seq.empty)
      case t: DecodeEnumType =>
        val prefixedEnumName = upperCamelCaseToUpperUnderscore(t.prefixedCTypeName)
        (Seq(t.cTypeDef(CEnumTypeDef(t.constants.map(c =>
          CEnumTypeDefConst(prefixedEnumName + "_" + c.name.asMangledString, c.value.toInt))))),
          Seq.empty)
      case t: DecodeGenericType =>
        (CAstElements(), CAstElements())
      case t: DecodeGenericTypeSpecialized => t.genericType.obj match {
        case optional: DecodeOptionalType =>
          require(t.genericTypeArguments.size == 1)
          val head = t.genericTypeArguments.head.getOrElse { sys.error("wtf") }
          head.obj match {
            // TODO: implement or remove
            // case h if h.isBasedOnEnum => (Seq(t.cTypeDef(head.obj.cType)), Seq.empty)
            case _ => (Seq(t.cTypeDef(CStructTypeDef(Seq(
              CStructTypeDefField("value", t.genericTypeArguments.head.get.obj.cType),
              CStructTypeDefField("flag", b8Type))))), Seq.empty)
          }
        case or: DecodeOrType =>
          var index = 0
          (Seq(t.cTypeDef(CStructTypeDef(t.genericTypeArguments.flatMap { ot =>
            index += 1
            ot.map(t => Seq(CStructTypeDefField("_" + index, t.obj.cType))).getOrElse(Seq.empty)
          } :+ tag.field))), Seq.empty)
      }
      case t: DecodeArrayType =>
        val arrayType: CType = config.arrayAsPointer match {
          case true => t.baseType.obj.cType.ptr
          case false => CArrayType(t.baseType.obj.cType, t.maxLength, dataVar.name)
        }
        val typeDef = CTypeDefStatement(t.prefixedCTypeName, CStructTypeDef(Seq(
          CStructTypeDefField(size.name, sizeTType),
          CStructTypeDefField(dataVar.name, arrayType))))
        val defineNamePrefix = upperCamelCaseToUpperUnderscore(t.prefixedCTypeName)
        (Seq(CDefine(defineNamePrefix + "_MIN_LEN", t.size.minLength.toString), CEol,
          CDefine(defineNamePrefix + "_MAX_LEN", t.maxLength.toString), CEol, CEol, typeDef), CAstElements())
      case t: DecodeStructType => (Seq(t.cTypeDef(CStructTypeDef(t.fields.map(f =>
        CStructTypeDefField(f.name.asMangledString, f.typeUnit.t.obj.cType))))), Seq.empty)
      case t: DecodeAliasType =>
        if (isAliasNameTheSame(t))
          (Seq.empty, Seq.empty)
        else
          (Seq(t.cTypeDef(t.baseType.obj.cType)), Seq.empty)
      case _ => sys.error(s"not implemented $t")
    }

    if (h.nonEmpty) {
      val importTypes = t.importTypes
      val imports = CAstElements(importTypes.filterNot(_.isPrimitiveOrNative).flatMap(t => CInclude(relPathForType(t)).eol): _*)

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

  private def generateRootComponent(comp: DecodeComponent) {
    logger.debug(s"Generating component ${comp.name.asMangledString}")
    config.isSingleton match {
      case true =>
        generateSingleton(comp)
      case _ =>
        sys.error("not implemented")
        val nsSet = mutable.HashSet.empty[DecodeNamespace]
        comp.collectNamespaces(nsSet)
        nsSet.foreach(generateNs)
        allComponentsSetForComponent(comp).foreach(generateComponent)
    }
  }

  private def importStatementsForComponent(comp: DecodeComponent): CAstElements = {
    val imports = comp.subComponents.flatMap(cr => CInclude(includePathForComponent(cr.component.obj)).eol).to[mutable.Buffer]
    if (imports.nonEmpty)
      imports += CEol
    val types = comp.allTypes.toSeq
    val typeIncludes = types.filterNot(_.isPrimitiveOrNative).flatMap(t => CInclude(relPathForType(t)).eol)
    imports ++= typeIncludes
    if (typeIncludes.nonEmpty)
      imports += CEol
    imports.to[immutable.Seq]
  }

  private def typeIncludes(component: DecodeComponent): CAstElements =
    component.allTypes.toSeq.filter(_.isGeneratable).flatMap(t => CInclude(relPathForType(t)).eol)

  private def generateSingleton(component: DecodeComponent): Unit = {

    component.allTypes.foreach(t => generateTypeSeparateFiles(t, ensureDirForNsExists(t.namespace)))

    val nsDir = ensureDirForNsExists(component.namespace)
    val componentStructName = component.prefixedTypeName
    val (hFile, cFile) = (new File(nsDir, componentStructName + headerExt),
      new File(nsDir, componentStructName + sourcesExt))

    val allComponentById = component.allComponentsById
    val guidDefines = Seq(CDefine("PHOTON_COMPONENTS", allComponentById.size.toString), CEol,
      CDefine("PHOTON_COMPONENT_IDS", '{' + allComponentById.keys.mkString(", ") + '}'), CEol) ++
      allComponentById.flatMap { case (id, c) =>
        val defineNamePrefix = upperCamelCaseToUpperUnderscore(c.prefixedTypeName)
        Seq(CDefine(defineNamePrefix + "_GUID", '"' + c.fqn.asMangledString + '"'), CEol,
          CDefine("PHOTON_COMPONENT_" + id + "_GUID", defineNamePrefix + "_GUID"), CEol,
          CDefine(defineNamePrefix + "_ID",  id.toString), CEol)
    }
    val methods = component.allMethods ++ component.allSubComponentsMethods

    hFile.write((CEol +: appendPrologEpilog((typeIncludes(component) ++
      "USER command implementation functions, MUST BE defined".comment ++
      component.commandMethodImplDefs.flatMap(m => Seq(CEol, m)).eol ++
      "USER parameter implementation functions, MUST BE defined".comment ++
      component.parameterMethodImplDefs.flatMap(m => Seq(CEol, m)).eol ++
      "Component defines".comment.eol ++ guidDefines ++
      "Message ID for component defines".comment.eol ++ component.allMessageDefines ++
      "Command ID for component defines".comment.eol ++ component.allCommandDefines ++
      "Implemented functions".comment ++ methods.map(_.definition).flatMap(m => Seq(CEol, m))).externC.eol))
      .protectDoubleInclude(dirPathForNs(component.namespace) + hFile.getName))

    cFile.write(CInclude(includePathForNsFileName(component.namespace, hFile.getName)).eol.eol ++
      methods.flatMap(m => m.eol.eol))
  }

  private def generateComponent(component: DecodeComponent) {
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

    val methods = component.allCommandsMethods ++ component.allParameterMethods ++ component.allMethods

    val externedCFile = (forwardFuncTableDecl.eol.eol ++ componentTypeForwardDecl.eol.eol ++
      Seq(componentType) ++ CSemicolon.eol ++ methods.flatMap(m => m.definition.eol)).externC
    hFile.writeIfNotEmptyWithComment((CEol +: appendPrologEpilog(imports ++ externedCFile))
      .protectDoubleInclude(dirPathForNs(component.namespace) + hFileName),
      s"Component ${component.name.asMangledString} interface")
    cFile.writeIfNotEmptyWithComment(CInclude(includePathForComponent(component)).eol.eol ++
      methods.flatMap(f => f.eol.eol), s"Component ${component.name.asMangledString} implementation")
  }

  private def structTypeFieldForCommand(structComponent: DecodeComponent, component: DecodeComponent,
                                        command: DecodeCommand): CStructTypeDefField = {
    val methodName = command.cStructFieldName(structComponent, component)
    CStructTypeDefField(methodName, CFuncType(command.returnType.map(_.obj.cType).getOrElse(voidType),
      command.cFuncParameterTypes(structComponent), methodName))
  }

  private def componentStructType(component: DecodeComponent): CStructTypeDef = {
    val componentSelfPtrType = component.ptrType
    CStructTypeDef(Seq(CStructTypeDefField("data", CTypeApplication(component.componentDataTypeName).ptr)) ++
      component.allCommands.map { case ComponentCommand(c, command) =>
        structTypeFieldForCommand(component, c, command)
      } ++
      component.allParameters.map { case ComponentParameterField(c, f) =>
        val name = f.cStructFieldName(component, c)
        CStructTypeDefField(
          name, CFuncType(f.typeUnit.t.obj.cType, Seq(componentSelfPtrType), name))
      } ++
      component.baseType.map(_.obj.fields.map { f =>
        val name = f.mangledCName
        CStructTypeDefField(name, CFuncType(f.typeUnit.t.obj.cType, Seq(componentSelfPtrType), name))
      }).getOrElse(Seq.empty) ++ component.commands.map(structTypeFieldForCommand(component, component, _)),
      Some(component.prefixedTypeName + structNamePostfix))
  }

  private def nsOrAliasCppSourceParts(ns: DecodeNamespace): Seq[String] =
    config.namespaceAliases.getOrElse(ns.fqn, Some(ns.fqn)).map(_.parts.map(_.asMangledString)).getOrElse(Seq.empty)

  private def dirPathForNs(ns: DecodeNamespace): String = nsOrAliasCppSourceParts(ns).mkString(io.File.separator)

  private def includePathForNsFileName(ns: DecodeNamespace, fileName: String): String =
    dirPathForNs(ns) + io.File.separator + fileName

  private def dirForNs(ns: DecodeNamespace): io.File = new io.File(config.outputDir, dirPathForNs(ns))

  private def relPathForType(t: DecodeType): String =
    dirPathForNs(t.namespace) + io.File.separator + t.fileName + headerExt

  private def includePathForComponent(comp: DecodeComponent): String =
    includePathForNsFileName(comp.namespace, comp.prefixedTypeName + headerExt)

  private def appendPrologEpilog(file: CAstElements): CAstElements = {
    val prefix = config.prologueEpiloguePath.map(_ + io.File.separator).getOrElse("")
    CInclude(prefix + "photon_prologue.h").eol.eol ++ file ++ Seq(CEol) ++
      CInclude(prefix + "photon_epilogue.h").eol.eol
  }

  private def generateTypeSeparateFiles(t: DecodeType, nsDir: io.File): Unit = if (!t.isPrimitiveOrNative) {
    val fileName = t.fileName
    val hFileName = fileName + headerExt
    val cFileName = fileName + sourcesExt
    val (hFile, cFile) = (new io.File(nsDir, hFileName), new io.File(nsDir, cFileName))
    val (h, c) = generateType(t, nsDir)
    if (h.nonEmpty)
      hFile.writeIfNotEmptyWithComment((CEol +: appendPrologEpilog(h).eol).protectDoubleInclude(relPathForType(t)),
        "Type header")
    else
      logger.debug(s"Omitting type ${t.optionName.toString}")
    if (c.nonEmpty)
      cFile.writeIfNotEmptyWithComment(CInclude(relPathForType(t)).eol.eol ++ c, "Type implementation")
  }

  private def collectNsForType[T <: DecodeType](t: DecodeMaybeProxy[T], set: mutable.Set[DecodeNamespace]) {
    require(t.isResolved, s"Proxy not resolved error for ${t.proxy.toString}")
    t.obj.collectNamespaces(set)
  }

  def mapIfSmall[A <: B, B <: CAstElement](el: A, t: DecodeType, f: A => B): B = if (t.isSmall) f(el) else el
  def mapIfNotSmall[A <: B, B <: CAstElement](el: A, t: DecodeType, f: A => B): B = if (t.isSmall) el else f(el)

  private def defineAndInitVar(v: CVar, parameter: DecodeCommandParameter): CAstElements = {
    val paramType = parameter.paramType.obj
    CStatements(v.define(paramType.cType), paramType.deserializeMethodName.call(v.ref, reader.v)._try)
  }

  private def tryCall(methodName: String, exprs: CExpression*): CFuncCall = methodName.call(exprs: _*)._try

  implicit class RichArrayType(val t: DecodeArrayType) {

    private def codeForArrayElements(expr: CExpression, codeGen: CExpression => CExpression, ref: Boolean): CAstElements = {
      val dataExpr = expr -> dataVar(i.v)
      Seq(CIndent, CForStatement(Seq(i.v.define(i.t, Some(CIntLiteral(0))), CComma, size.v.assign(expr -> size.v)),
        Seq(CLess(i.v, size.v)), Seq(CIncBefore(i.v)), Seq(codeGen(if (ref) dataExpr.ref else dataExpr).line)), CEol)
    }

    def serializeCodeForArrayElements(src: CExpression): CAstElements = {
      val baseType = t.baseType.obj
      codeForArrayElements(src, baseType.serializeCallCode, ref = !baseType.isSmall)
    }

    def deserializeCodeForArrayElements(dest: CExpression): CAstElements = {
      val baseType = t.baseType.obj
      codeForArrayElements(dest, baseType.deserializeCallCode, ref = true)
    }

    def maxLength: Long = {
      val size = t.size
      if (t.isFixedSize)
        size.minLength.min(256)
      if (size.maxLength == 0)
        256
      else
        size.maxLength.min(256)
    }
  }

  implicit class RichType(val t: DecodeType) {

    import RichType._

    def cMethodReturnType: CType = if (t.isSmall) t.cType else t.cType.ptr.const

    def cMethodReturnParameters: Seq[CFuncParam] = Seq.empty //if (t.isSmall) Seq.empty else Seq(CFuncParam("result", t.cType.ptr))

    def cType: CType = CTypeApplication(t.prefixedCTypeName)

    def isPrimitiveOrNative = t match {
      case _: DecodePrimitiveType | _: DecodeNativeType => true
      case _ => false
    }

    def fileName: String = t.prefixedCTypeName

    def prefixedCTypeName: String = t match {
      case _: DecodePrimitiveType => cTypeName
      case _: DecodeNativeType => "Photon" + cTypeName
      case _ => "PhotonGt" + cTypeName
    }

    def isBasedOnEnum: Boolean = t match {
      case _: DecodeEnumType => true
      case _: DecodeArrayType => false
      case t: BaseTyped => t.baseType.obj.isBasedOnEnum
      case _ => false
    }

    def cTypeName: String = t match {
      case t: DecodeNamed => lowerUnderscoreToUpperCamel(t.name.asMangledString)
      case t: DecodePrimitiveType => primitiveTypeToCTypeApplication(t).name
      case t: DecodeArrayType =>
        val baseCType = t.baseType.obj.cTypeName
        val min = t.size.minLength
        val max = t.size.maxLength
        "Arr" + baseCType + ((t.isFixedSize, min, max) match {
          case (true, 0, _) | (false, 0, 0) => ""
          case (true, _, _) => s"Fixed$min"
          case (false, 0, _) => s"Max$max"
          case (false, _, 0) => s"Min$min"
          case (false, _, _) => s"Min${min}Max$max"
        })
      case t: DecodeGenericTypeSpecialized =>
        t.genericType.obj.cTypeName +
          t.genericTypeArguments.map(_.map(_.obj.cTypeName).getOrElse("Void")).mkString
      // fixme: remove asInstanceOf
      case t: DecodeOptionNamed => lowerUnderscoreToUpperCamel(t.asInstanceOf[DecodeOptionNamed].cTypeName)
      case _ => sys.error("not implemented")
    }

    def isGeneratable: Boolean = t match {
      case _ if isPrimitiveOrNative => false
      case t: DecodeGenericType => false
      case _ => true
    }

    def byteSize: Int = t match {
      case t: DecodePrimitiveType => (t.bitLength / 8).toInt
      case t: DecodeNativeType => t match {
        case t: DecodeBerType => BER_BYTE_SIZE
        case _ => sys.error(s"not implemented for $t")
      }
      case t: DecodeArrayType => (t.maxLength * t.baseType.obj.byteSize).toInt
      case t: DecodeStructType => t.fields.map(_.typeUnit.t.obj.byteSize).sum
      case t: BaseTyped => t.baseType.obj.byteSize
      case t: DecodeGenericTypeSpecialized => t.genericType.obj match {
        case _: DecodeOptionalType => 1 + t.genericTypeArguments.head.getOrElse { sys.error("wtf") }.obj.byteSize
        case _: DecodeOrType => 1 + t.genericTypeArguments.map(_.map(_.obj.byteSize).getOrElse(0)).max
        case _ => sys.error(s"not implemented for $t")
      }
      case _ => sys.error(s"not implemented for $t")
    }

    def isSmall: Boolean = t.byteSize <= 16

    def typeWithDependentTypes: immutable.Set[DecodeType] =
      (t match {
        case t: DecodeStructType => t.fields.flatMap(_.typeUnit.t.obj.typeWithDependentTypes).toSet
        case t: BaseTyped => t.baseType.obj.typeWithDependentTypes
        case t: DecodeGenericTypeSpecialized =>
          t.genericTypeArguments.flatMap(_.map(_.obj.typeWithDependentTypes).getOrElse(Set.empty)).toSet ++
            t.genericType.obj.t.typeWithDependentTypes
        case _: DecodeNativeType | _: DecodeGenericType | _: DecodePrimitiveType => Set.empty[DecodeType]
        case _ => sys.error(s"not implemented for $t")
      }) + t

    def methodName(name: String): String = t.prefixedCTypeName.methodName(name)

    private val berSizeOf = "sizeof".call(berType)

    def abstractMinSizeExpr: Option[CExpression] = t match {
      case t: DecodeBerType => Some(berSizeOf)
      case t: DecodeAliasType => t.baseType.obj.abstractMinSizeExpr
      case t: DecodeSubType => t.baseType.obj.abstractMinSizeExpr
      case t: DecodePrimitiveType => Some("sizeof".call(t.cType))
      case t: DecodeStructType => t.fields.map { f => f.typeUnit.t.obj.abstractMinSizeExpr }.foldLeft[Option[CExpression]](None) {
        (l: Option[CExpression], r: Option[CExpression]) =>
          l.map { lExpr => r.map { rExpr => CPlus(lExpr, rExpr) }.getOrElse(lExpr) }.orElse(r)
      }
      case t: DecodeArrayType =>
        t.baseType.obj.abstractMinSizeExpr.map{ rExpr => CPlus(berSizeOf, rExpr) }.orElse(Some(berSizeOf))
      case _ => sys.error(s"not implemented for $t")
    }

    def concreteMinSizeExpr(src: CExpression): Option[CExpression] = t match {
      case t: DecodeStructType => t.fields.map { f => f.typeUnit.t.obj.concreteMinSizeExpr(src.dot(f.cName._var)) }.foldLeft[Option[CExpression]](None) {
        (l: Option[CExpression], r: Option[CExpression]) =>
          l.map { lExpr => r.map { rExpr => CPlus(lExpr, rExpr) }.getOrElse(lExpr) }.orElse(r)
      }
      case t: DecodeArrayType =>
        t.baseType.obj.abstractMinSizeExpr.map{ rExpr => CMul(src.dot(size.v), rExpr) }.orElse(None)
      case _: DecodeSubType | _: DecodeAliasType | _: DecodeGenericTypeSpecialized => None // todo: yes you can
      case t: DecodeEnumType => t.baseType.obj.concreteMinSizeExpr(src)
      case _ => abstractMinSizeExpr
    }

    private def writerSizeCheckCode(src: CExpression) = concreteMinSizeExpr(CParens(src.deref)).map { sizeExpr =>
      Seq(CIndent, CIf(CLess("PhotonWriter_WritableSize".call(writer.v), sizeExpr),
        Seq(CEol, CReturn("PhotonResult_NotEnoughSpace"._var).line)))
    }.getOrElse(Seq.empty)

    private def readerSizeCheckCode(dest: CExpression) = concreteMinSizeExpr(CParens(dest.deref)).map { sizeExpr =>
      Seq(CIndent, CIf(CLess("PhotonReader_ReadableSize".call(reader.v), sizeExpr),
        Seq(CEol, CReturn("PhotonResult_NotEnoughData"._var).line)))
    }.getOrElse(Seq.empty)

    def serializeMethodName: String = methodName(typeSerializeMethodName)

    def deserializeMethodName: String = methodName(typeDeserializeMethodName)

    private def trySerializeCode(src: CExpression): CFuncCall =
      tryCall(methodName(typeSerializeMethodName), src, writer.v)

    private def tryDeserializeCode(dest: CExpression): CFuncCall =
      tryCall(methodName(typeDeserializeMethodName), dest, reader.v)

    private def callCodeForBer(methodNamePart: String, exprs: CExpression*): CExpression = tryCall(
      photonBerTypeName.methodName(methodNamePart), exprs: _*)

    def serializeBerCallCode(src: CExpression): CExpression = callCodeForBer(typeSerializeMethodName, src, writer.v)

    def deserializeBerCallCode(dest: CExpression): CExpression = callCodeForBer(typeDeserializeMethodName, dest, reader.v)

    def serializeCallCode(src: CExpression): CExpression = t match {
      case _: DecodeArrayType | _: DecodeStructType => trySerializeCode(src)
      case t: DecodeNativeType => t match {
        case t: DecodeBerType => callCodeForBer(typeSerializeMethodName, src, writer.v)
        case _ => sys.error(s"not implemented for $t")
      }
      case t: BaseTyped => t.baseType.obj.serializeCallCode(src)
      case t: DecodePrimitiveType =>
        callCodeForPrimitiveType(t, src, photonWriterTypeName, "Write", writer.v, src)
      case _ => sys.error(s"not implemented for $t")
    }

    def deserializeCallCode(dest: CExpression): CExpression = t match {
      case _: DecodeArrayType | _: DecodeStructType => tryDeserializeCode(dest)
      case t: DecodeNativeType => t match {
        case t: DecodeBerType => callCodeForBer(typeDeserializeMethodName, dest, reader.v)
        case _ => sys.error(s"not implemented for $t")
      }
      case t: BaseTyped =>
        val baseType = t.baseType.obj
        baseType.deserializeCallCode(dest.cast(baseType.cType.ptr))
      case t: DecodePrimitiveType =>
        CAssign(CDeref(dest), callCodeForPrimitiveType(t, dest, photonReaderTypeName, "Read", reader.v))
      case _ => sys.error(s"not implemented for $t")
    }

    private val flagVar: CVar = "flag"._var
    private val valueVar: CVar = "value"._var
    private val tagVar: CVar = "tag"._var

    private def serializeGenericTypeSpecializedCode(t: DecodeGenericTypeSpecialized, src: CExpression): CAstElements = {
      val isSmall = t.isSmall
      t.genericType.obj match {
        case _: DecodeOrType =>
          val tagField = if (isSmall) src.dot(tagVar) else src -> tagVar
          photonBerTypeName.methodName(typeSerializeMethodName).call(tagField, writer.v)._try.line +:
            Seq(CIndent, CSwitch(tagField, t.genericTypeArguments.zipWithIndex.map { case (omp, idx) =>
              CCase(CIntLiteral(idx), omp.map { mp =>
                val valueVar = ("_" + (idx + 1))._var
                Seq(mp.obj.serializeCallCode(
                  mapIfNotSmall(if (isSmall) src.dot(valueVar) else src -> valueVar, mp.obj,
                    (expr: CExpression) => expr.ref)).line, CIndent, CBreak, CSemicolon, CEol)
              }.getOrElse {
                Seq(CStatementLine(CBreak, CSemicolon))
              })
            }, default = CStatements(CReturn(invalidValue))), CEol)
        case _: DecodeOptionalType =>
          val flagField = if (isSmall) src.dot(flagVar) else src -> flagVar
          photonBerTypeName.methodName(typeSerializeMethodName).call(
            flagField, writer.v)._try.line +:
            Seq(CIndent, CIf(flagField, CEol +:
              t.genericTypeArguments.head.getOrElse {
                sys.error("wtf")
              }.obj.serializeCode(
                if (isSmall) src.dot(valueVar) else src -> valueVar)))
        case _ => sys.error(s"not implemented $t")
      }
    }

    private def deserializeGenericTypeSpecializedCode(t: DecodeGenericTypeSpecialized, dest: CExpression): CAstElements =
      t.genericType.obj match {
        case _: DecodeOrType =>
          photonBerTypeName.methodName(typeDeserializeMethodName).call((dest -> tagVar).ref, reader.v)._try.line +:
            Seq(CIndent, CSwitch(dest -> tagVar, t.genericTypeArguments.zipWithIndex.map{ case (omp, idx) =>
              CCase(CIntLiteral(idx), omp.map { mp =>
                Seq(mp.obj.deserializeCallCode((dest -> ("_" + (idx + 1))._var).ref).line,
                  CIndent, CBreak, CSemicolon, CEol)
              }.getOrElse { Seq(CStatementLine(CBreak, CSemicolon)) })
            }, default = CStatements(CReturn(invalidValue))), CEol)
        case _: DecodeOptionalType =>
          photonBerTypeName.methodName(typeDeserializeMethodName).call(
            (dest -> flagVar).ref.cast(CTypeApplication(photonBerTypeName).ptr), reader.v)._try.line +:
            Seq(CIndent, CIf(dest -> flagVar, CEol +:
              t.genericTypeArguments.head.getOrElse{ sys.error("wtf") }.obj.deserializeCode((dest -> valueVar).ref)))
        case _ => sys.error(s"not implemented $t")
      }

    def serializeCode: CAstElements = serializeCode(selfVar)

    def serializeCode(src: CExpression): CAstElements = t match {
      case t: DecodeStructType => writerSizeCheckCode(src) ++ t.fields.flatMap { f =>
        val fType = f.typeUnit.t.obj
        val fVar = f.cName._var
        Seq(fType.serializeCallCode((if (t.isSmall) src.dot(fVar) else src -> fVar).refIfNotSmall(fType)).line)
      }
      case t: DecodeArrayType =>
        writerSizeCheckCode(src) ++ src.serializeCodeForArraySize ++ t.serializeCodeForArrayElements(src)
      case t: DecodeAliasType => Seq(t.baseType.obj.serializeCallCode(src).line)
      case t: DecodeSubType => Seq(t.baseType.obj.serializeCallCode(src).line)
      case t: DecodeEnumType => Seq(t.baseType.obj.serializeCallCode(src).line)
      case t: DecodePrimitiveType => Seq(t.serializeCallCode(src).line)
      case t: DecodeNativeType => Seq(t.serializeCallCode(src).line)
      case t: DecodeGenericTypeSpecialized => serializeGenericTypeSpecializedCode(t, src)
      case _ => sys.error(s"not implemented for $t")
    }

    def deserializeCode: CAstElements = deserializeCode(selfVar)

    def deserializeCode(dest: CExpression): CAstElements = t match {
      case t: DecodeStructType => t.fields.flatMap { f =>
        Seq(f.typeUnit.t.obj.deserializeCallCode((dest -> f.cName._var).ref).line)
      }
      case t: DecodeArrayType =>
        dest.deserializeCodeForArraySize ++ readerSizeCheckCode(dest) ++ t.deserializeCodeForArrayElements(dest)
      case t: DecodeAliasType => t.baseType.obj.deserializeCode(dest)
      case t: BaseTyped =>
        val baseType = t.baseType.obj
        Seq(baseType.deserializeCallCode(dest.cast(baseType.cType.ptr)).line)
      case _: DecodePrimitiveType | _: DecodeNativeType => Seq(t.deserializeCallCode(dest).line)
      case t: DecodeGenericTypeSpecialized => deserializeGenericTypeSpecializedCode(t, dest)
      case _ => sys.error(s"not implemented for $t")
    }

    def cTypeDef(cType: CType) = CTypeDefStatement(t.prefixedCTypeName, cType)

    def collectNamespaces(set: mutable.Set[DecodeNamespace]) {
      set += t.namespace
      t match {
        case t: BaseTyped => collectNsForType(t.baseType, set)
        case t: DecodeStructType => t.fields.foreach(f => collectNsForType(f.typeUnit.t, set))
        case t: DecodeGenericTypeSpecialized => t.genericTypeArguments.foreach(_.foreach(collectNsForType(_, set)))
        case _ =>
      }
    }

    def importTypes: Seq[DecodeType] = t match {
      case t: DecodeStructType => t.fields.flatMap { f =>
        val t = f.typeUnit.t.obj
        if (t.isPrimitiveOrNative)
          Seq.empty
        else
          Seq(t)
      }
      case s: DecodeGenericTypeSpecialized =>
        s.genericType.obj match {
          case optional: DecodeOptionalType =>
            Seq(s.genericTypeArguments.head.getOrElse{ sys.error("invalid optional types") }.obj)
          case or: DecodeOrType =>
            s.genericTypeArguments.flatMap(_.map(p => Seq(p.obj)).getOrElse(Seq.empty))
        }
      case t: BaseTyped =>
        if (t.baseType.obj.isPrimitiveOrNative)
          Seq.empty
        else
          Seq(t.baseType.obj)
      case _ => Seq.empty
    }
  }

  implicit class RichComponent(val component: DecodeComponent) {
    def allCommands: Seq[WithComponent[DecodeCommand]] =
      allSubComponents.toSeq.flatMap(sc => sc.commands.map(ComponentCommand(sc, _)))

    def allParameters: Seq[ComponentParameterField] =
      allSubComponents.toSeq.flatMap(sc => sc.baseType.map(_.obj.fields.map(ComponentParameterField(sc, _)))
        .getOrElse(Seq.empty))

    def typeName: String = component.name.asMangledString

    def executeCommandMethodNamePart: String = "ExecuteCommand"

    def functionForCommandMethodName: String = component.prefixedTypeName.methodName("FunctionForCommand")

    def executeCommandMethodName(rootComponent: DecodeComponent): String =
      if (component == rootComponent)
        component.prefixedTypeName.methodName(executeCommandMethodNamePart)
      else
        rootComponent.prefixedTypeName.methodName(component.cName + executeCommandMethodNamePart)

    def readExecuteCommandMethodNamePart: String = "ReadExecuteCommand"

    def readExecuteCommandMethodName(rootComponent: DecodeComponent): String =
      if (component == rootComponent)
        component.prefixedTypeName.methodName(readExecuteCommandMethodNamePart)
      else
        rootComponent.prefixedTypeName.methodName(component.cName + readExecuteCommandMethodNamePart)

    def writeMessageMethodName(rootComponent: DecodeComponent): String =
      if (component == rootComponent)
        component.prefixedTypeName.methodName("WriteMessage")
      else
        rootComponent.prefixedTypeName.methodName(component.cName + "WriteMessage")

    def isStatusMessageMethodName: String = component.prefixedTypeName.methodName("IsStatusMessage")

    def prefixedTypeName: String = typePrefix + typeName

    def componentDataTypeName: String = prefixedTypeName + "Data"

    def ptrType: CPtrType = CTypeApplication(prefixedTypeName).ptr

    def functionTableTypeName: String = prefixedTypeName + "UserFunctionTable"

    def allSubComponents: immutable.Set[DecodeComponent] =
      component.subComponents.flatMap { ref =>
        val c = ref.component.obj
        c.allSubComponents + c
      }.toSet

    def collectNamespaces(nsSet: mutable.HashSet[DecodeNamespace]) {
      component.subComponents.foreach(_.component.obj.collectNamespaces(nsSet))
      collectNsForTypes(nsSet)
    }

    def collectNsForTypes(set: mutable.Set[DecodeNamespace]) {
      for (baseType <- component.baseType)
        collectNsForType(baseType, set)
      component.commands.foreach { cmd =>
        cmd.parameters.foreach(arg => collectNsForType(arg.paramType, set))
        for (returnType <- cmd.returnType)
          collectNsForType(returnType, set)
      }
    }

    def selfParam: CFuncParam = CFuncParam(selfVar.name, component.ptrType)

    def writeMessageMethod: CFuncImpl = writeMessageMethod(component)

    def writeMessageMethod(rootComponent: DecodeComponent): CFuncImpl = {
      val selfParam = CFuncParam(selfVar.name, CTypeApplication(rootComponent.prefixedTypeName).ptr)
      CFuncImpl(CFuncDef(component.writeMessageMethodName(rootComponent), resultType,
        Seq(selfParam, writer.param, messageId.param)),
        Seq(messageId.v.serializeBer._try.line, CIndent, CSwitch(messageId.v,
          casesForMap(component.allMessagesById, (message: DecodeMessage, c: DecodeComponent) =>
            CStatements(CReturn(message.fullMethodName(rootComponent, c).call(selfVar, writer.v)))),
          default = CStatements(CReturn(invalidMessageId))), CEol))
    }

    def isStatusMessageMethod: CFuncImpl = isStatusMessageMethod(component)

    def isStatusMessageMethod(rootComponent: DecodeComponent) =
      CFuncImpl(CFuncDef(component.isStatusMessageMethodName, b8Type,
        Seq(messageId.param)), Seq(CIndent, CSwitch(messageId.v, casesForMap(component.allMessagesById,
        (message: DecodeMessage, c: DecodeComponent) =>
          CStatements(CReturn(message.isInstanceOf[DecodeStatusMessage].toString._var))),
        default = CStatements(CReturn("false"._var))), CEol))

    def executeCommandMethod: CFuncImpl = executeCommandMethod(component)

    def executeCommandMethod(rootComponent: DecodeComponent): CFuncImpl = {
      val selfParam = CFuncParam(selfVar.name, CTypeApplication(rootComponent.prefixedTypeName).ptr)
      CFuncImpl(CFuncDef(component.executeCommandMethodName(rootComponent), resultType,
        Seq(selfParam, reader.param, writer.param, commandId.param)),
        Seq(CIndent, CSwitch(commandId.v, casesForMap(component.allCommandsById,
          (command: DecodeCommand, c: DecodeComponent) =>
            CStatements(CReturn(command.executeMethodName(rootComponent, c).call(selfVar, reader.v, writer.v)))),
          default = CStatements(CReturn(invalidCommandId))), CEol))
    }

    def readExecuteCommandMethod: CFuncImpl = readExecuteCommandMethod(component)

    def readExecuteCommandMethod(rootComponent: DecodeComponent): CFuncImpl = {
      val selfParam = CFuncParam(selfVar.name, CTypeApplication(rootComponent.prefixedTypeName).ptr)
      CFuncImpl(CFuncDef(component.readExecuteCommandMethodName(rootComponent), resultType,
        Seq(selfParam, reader.param, writer.param)),
        CStatements(commandId.v.define(commandId.t),
          tryCall(photonBerTypeName.methodName(typeDeserializeMethodName), commandId.v.ref, reader.v),
          CReturn(CFuncCall(component.executeCommandMethodName(rootComponent), selfVar, reader.v, writer.v, commandId.v))))
    }

    def executeCommandForComponentMethodNamePart: String = "ExecuteCommandForComponent"

    def executeCommandForComponentMethodName(rootComponent: DecodeComponent): String =
      if (component == rootComponent)
        component.prefixedTypeName.methodName(executeCommandForComponentMethodNamePart)
      else
        rootComponent.prefixedTypeName.methodName(component.cName + executeCommandForComponentMethodNamePart)

    def executeCommandForComponentMethod(rootComponent: DecodeComponent): CFuncImpl = {
      val selfParam = CFuncParam(selfVar.name, CTypeApplication(rootComponent.prefixedTypeName).ptr)
      CFuncImpl(CFuncDef(component.executeCommandForComponentMethodName(rootComponent), resultType,
        Seq(selfParam, reader.param, writer.param, componentId.param, commandId.param)),
        Seq(CIndent, CSwitch(componentId.v, component.allComponentsById.toSeq.sortBy(_._1).map { case (id, c) =>
          CCase(CIntLiteral(id), CStatements(CReturn(c.readExecuteCommandMethodName(rootComponent)
            .call(selfParam.name._var, reader.v, writer.v))))
        }, default = CStatements(CReturn(invalidComponentId))), CEol))
    }

    def executeCommandForComponentMethod: CFuncImpl = executeCommandForComponentMethod(component)

    def allMethods: Seq[CFuncImpl] = component.allCommandsMethods ++ component.allParameterMethods ++
      Seq(component.executeCommandMethod, component.readExecuteCommandMethod, component.writeMessageMethod,
        component.isStatusMessageMethod) ++
      component.allSubComponents.map(_.executeCommandForComponentMethod(component))

    def parameterMethodName(parameter: DecodeMessageParameter, rootComponent: DecodeComponent): String =
      rootComponent.prefixedTypeName.methodName(parameter.ref(component).structField
        .getOrElse { sys.error("not implemented") }.cStructFieldName(rootComponent, component))

    def allParameterMethods: Seq[CFuncImpl] = {
      val componentPtrType = component.ptrType
      allMessagesById.toSeq.sortBy(_._1).map(_._2).map { case ComponentMessage(c, message) =>
        CFuncImpl(CFuncDef(message.fullMethodName(component, c), resultType,
          Seq(CFuncParam(selfVar.name, componentPtrType), writer.param)),
          message.parameters.map { p =>
            val v = p.varName._var
            val parameterRef = p.ref(c)
            val structField = parameterRef.structField.getOrElse { sys.error("not implemented") }
            val t = structField.typeUnit.t.obj
            v.define(mapIfNotSmall(t.cType, t, (ct: CType) => ct.ptr.const),
              Some(c.parameterMethodName(p, component).call(selfVar))).line
          } ++
            message.parameters.flatMap { p =>
              val v = p.varName._var
              val parameterRef = p.ref(c)
              if (parameterRef.structField.isDefined) {
                val t = parameterRef.t
                if (parameterRef.subTokens.isEmpty)
                  Seq(t.serializeCallCode(v).line)
                else
                  sys.error("not implemented")
              } else {
                sys.error("not implemented")
              }
            } :+
            CReturn(resultOk).line)
      }
    }

    def allCommandDefines: CAstElements = allCommandDefines(component)

    def allCommandDefines(rootComponent: DecodeComponent): CAstElements =
      component.commandDefines(rootComponent) ++ component.allSubComponents.toSeq.flatMap(_.commandDefines(component))

    def commandDefines(rootComponent: DecodeComponent): CAstElements = {
      val defineName: String = upperCamelCaseToUpperUnderscore(rootComponent.prefixedTypeName +
        (if (rootComponent == component) "" else component.cName) + "CommandIds")
      val commandById = component.allCommandsById
      CDefine(defineName + "_LEN", commandById.size.toString).eol ++
        CDefine(defineName, "{" + commandById.toSeq.map(_._1).sorted.mkString(", ") + "}").eol
    }

    def allMessageDefines: CAstElements = allMessageDefines(component)

    def allMessageDefines(rootComponent: DecodeComponent): CAstElements =
      component.messageDefines(rootComponent) ++ component.allSubComponents.toSeq.flatMap(_.messageDefines(component))

    def messageDefines(rootComponent: DecodeComponent): CAstElements = {
      val prefix = rootComponent.prefixedTypeName +
        (if (rootComponent == component) "" else component.cName)
      val idsDefineName = upperCamelCaseToUpperUnderscore(prefix + "MessageIds")
      val prioritiesDefineName = upperCamelCaseToUpperUnderscore(prefix + "MessagePriorities")
      val messageById = component.allMessagesById
      val messagesSortedById = messageById.toSeq.sortBy(_._1)
      CDefine(idsDefineName + "_LEN", messageById.size.toString).eol ++
        CDefine(idsDefineName, "{" + messagesSortedById.map(_._1).mkString(", ") + "}").eol ++
        CDefine(prioritiesDefineName, "{" + messagesSortedById.map(_._2._2 match {
          case m: DecodeStatusMessage => m.priority.getOrElse(0)
          case _ => 0
        }).mkString(", ") + "}").eol
    }

    def parameterMethodImplDefs: Seq[CFuncDef] = {
      val componentTypeName = component.prefixedTypeName
      val componentTypePtr = CTypeApplication(componentTypeName).ptr
      component.allParameters.map { case ComponentParameterField(c, f) =>
        val fType = f.typeUnit.t.obj
        CFuncDef(componentTypeName.methodName(f, component, c), fType.cMethodReturnType,
          Seq(CFuncParam(selfVar.name, componentTypePtr)) ++ fType.cMethodReturnParameters)
      }
    }

    def commandMethodImplDefs: Seq[CFuncDef] = {
      val componentTypeName = component.prefixedTypeName
      val componentTypePtr = CTypeApplication(componentTypeName).ptr
      component.allCommands.map { case ComponentCommand(c, command) =>
        CFuncDef(componentTypeName.methodName(command, component, c),
          command.returnType.map(_.obj.cMethodReturnType).getOrElse(voidType),
          Seq(CFuncParam(selfVar.name, componentTypePtr)) ++ command.parameters.map(p => {
            val t = p.paramType.obj
            CFuncParam(p.cName, mapIfNotSmall(t.cType, t, (ct: CType) => ct.ptr))
          }) ++ command.returnType.map(_.obj.cMethodReturnParameters).getOrElse(Seq.empty))
      }
    }

    def allSubComponentsMethods: Seq[CFuncImpl] = {
      component.allSubComponents.toSeq.flatMap { subComponent =>
        Seq(subComponent.executeCommandMethod(component), subComponent.readExecuteCommandMethod(component),
          subComponent.writeMessageMethod(component), subComponent.isStatusMessageMethod(component))
      }
    }

    def allCommandsMethods: Seq[CFuncImpl] = {
      val componentTypeName = component.prefixedTypeName
      val compType = component.ptrType
      val parameters = Seq(CFuncParam(selfVar.name, compType), reader.param, writer.param)
      component.allCommandsById.toSeq.sortBy(_._1).map(_._2).map { case ComponentCommand(subComponent, command) =>
        val methodNamePart = command.executeMethodNamePart(component, subComponent)
        val vars = command.parameters.map { p => CVar(p.mangledCName) }
        val varInits = vars.zip(command.parameters).flatMap { case (v, parameter) =>
          defineAndInitVar(v, parameter)
        }
        val cmdResultVar = CVar("cmdResult")
        val cmdReturnType = command.returnType.map(_.obj)
        val cmdCReturnType = command.returnType.map(_.obj.cType)
        val funcCall = command.methodName(component, subComponent).call(selfVar +:
          vars.zip(command.parameters.map(_.paramType.obj)).map{ case (v, t) => v.refIfNotSmall(t) }: _*)
        CFuncImpl(CFuncDef(componentTypeName.methodName(methodNamePart), resultType, parameters),
          varInits ++ cmdCReturnType.map(t => CStatements(cmdResultVar.define(
            mapIfNotSmall(t, cmdReturnType.getOrElse { sys.error("wtf") }, (ct: CType) => ct.ptr.const), Some(funcCall)),
            CReturn(cmdReturnType.getOrElse{ sys.error("not implemented") }.methodName(typeSerializeMethodName).call(
              cmdResultVar, writer.v))))
            .getOrElse(CStatements(funcCall, CReturn(resultOk))))
      }
    }

    def allTypes: immutable.Set[DecodeType] =
      (component.commands.flatMap(cmd => cmd.returnType.map(_.obj.typeWithDependentTypes).getOrElse(Seq.empty) ++
        cmd.parameters.flatMap(_.paramType.obj.typeWithDependentTypes)) ++
        component.baseType.map(_.obj.fields.flatMap(_.typeUnit.t.obj.typeWithDependentTypes)).getOrElse(Seq.empty)).toSet ++
        component.allSubComponents.flatMap(_.allTypes)

    // todo: optimize: memoize
    private def makeMapById[T <: DecodeHasOptionId](seq: Seq[T], subSeq: DecodeComponent => Seq[T])
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

    def allCommandsById: immutable.HashMap[Int, WithComponent[DecodeCommand]] =
      makeMapById(component.commands, _.commands)

    def allMessagesById: immutable.HashMap[Int, WithComponent[DecodeMessage]] =
      makeMapById(component.messages, _.messages)

    def allComponentsById: immutable.HashMap[Int, DecodeComponent] = {
      val map = mutable.HashMap.empty[Int, DecodeComponent]
      var nextId = 0
      val (withId, withoutId) = (component +: component.allSubComponents.toSeq).span(_.id.isDefined)
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

  implicit class RichCommand(val command: DecodeCommand) {
    def cFuncParameterTypes(component: DecodeComponent): Seq[CType] = {
      component.ptrType +: command.parameters.map(p => {
        val t = p.paramType.obj
        mapIfNotSmall(t.cType, t, (ct: CType) => ct.ptr)
      })
    }
  }

  implicit class RichMessage(val message: DecodeMessage) {
    def fullMethodName(rootComponent: DecodeComponent, component: DecodeComponent): String =
      rootComponent.prefixedTypeName.methodName("Write" + message.methodNamePart(rootComponent, component).capitalize)
  }

  implicit class RichNamed(val named: DecodeNamed) {

    def executeMethodNamePart(rootComponent: DecodeComponent, component: DecodeComponent): String =
      "Execute" + methodNamePart(rootComponent, component).capitalize

    def executeMethodName(rootComponent: DecodeComponent, component: DecodeComponent): String =
      rootComponent.prefixedTypeName.methodName(executeMethodNamePart(rootComponent, component))

    def methodNamePart(rootComponent: DecodeComponent, component: DecodeComponent): String =
      upperCamelCaseToLowerCamelCase((if (rootComponent == component) "" else component.typeName) +
        cName.capitalize)

    def methodName(rootComponent: DecodeComponent, component: DecodeComponent): String =
      rootComponent.prefixedTypeName.methodName(methodNamePart(rootComponent, component))

    def cName: String = named.name.asMangledString

    def mangledCName: String = {
      var methodName = cName
      if (keywords.contains(methodName))
        methodName = "_" + methodName
      methodName
    }

    def cStructFieldName(structComponent: DecodeComponent, component: DecodeComponent): String =
      upperCamelCaseToLowerCamelCase((if (structComponent == component) "" else component.cName) +
        named.mangledCName.capitalize)
  }

  implicit class RichString(val str: String) {
    def _var: CVar = CVar(str)

    def call(exprs: CExpression*) = CFuncCall(str, exprs: _*)

    def tryCall(exprs: CExpression*) = str.call(exprs: _*)._try

    def methodName(name: String): String = str + "_" + name.capitalize

    def initMethodName: String = methodName(typeInitMethodName)

    def methodName(command: DecodeCommand, rootComponent: DecodeComponent, component: DecodeComponent): String =
      methodName(command.methodNamePart(rootComponent, component))

    def methodName(f: DecodeStructField, rootComponent: DecodeComponent, component: DecodeComponent): String =
      methodName(f.methodNamePart(rootComponent, component))

    def comment: CAstElements = Seq(CEol, CComment(str), CEol)
  }

  private object RichType {
    def callCodeForPrimitiveType(t: DecodePrimitiveType, src: CExpression, typeName: String, methodPrefix: String,
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

    private def _codeForArraySize(methodName: String, expr2: CExpression, ref: Boolean): CAstElements = {
      val sizeExpr = expr -> size.v
      CStatements(photonBerTypeName.methodName(methodName).call(
        Seq(if (ref) sizeExpr.ref else sizeExpr, expr2): _*)._try)
    }

    def serializeCodeForArraySize: CAstElements =
      _codeForArraySize(typeSerializeMethodName, writer.v, ref = false)

    def deserializeCodeForArraySize: CAstElements =
      _codeForArraySize(typeDeserializeMethodName, reader.v, ref = true)
  }

  implicit class RichVar(val v: CVar) {
    def define(t: CType, init: Option[CExpression] = None, static: Boolean = false) = CVarDef(v.name, t, init, static)

    def serializeBer: CFuncCall = photonBerTypeName.methodName(typeSerializeMethodName).call(v, writer.v)
  }

  implicit class RichCAstElements(val els: CAstElements) {
    private val rand = new Random()

    def protectDoubleInclude(filePath: String): CAstElements = {
      //val bytes = new Array[Byte](20)
      //rand.nextBytes(bytes)
      val uniqueName = "__" + filePath.split(io.File.separatorChar).map(p =>
        upperCamelCaseToUpperUnderscore(p).replaceAll("\\.", "_")).mkString("_") + "__"
      /*"_" + MessageDigest.getInstance("MD5").digest(bytes).map("%02x".format(_)).mkString +*/
      "DO NOT EDIT! FILE IS AUTO GENERATED".comment.eol ++ Seq(CIfNDef(uniqueName), CEol, CDefine(uniqueName)) ++ els :+ CEndIf
    }

    def externC: CAstElements =
      Seq(CIfDef(cppDefine), CEol, CPlainText("extern \"C\" {"), CEol, CEndIf, CEol, CEol) ++ els ++
        Seq(CEol, CEol, CIfDef(cppDefine), CEol, CPlainText("}"), CEol, CEndIf)

    def eol: CAstElements = els :+ CEol
  }
}

private case class TypedVar(name: String, t: CType) {
  val v = CVar(name)
  val param = CFuncParam(name, t)
  val field = CStructTypeDefField(name, t)
}

case class ComponentParameterField(component: DecodeComponent, field: DecodeStructField)

object CSourcesGenerator {

  class WithComponent[T](val component: DecodeComponent, val _2: T)

  object WithComponent {
    def apply[T](component: DecodeComponent, _2: T) = new WithComponent[T](component, _2)

    def unapply[T](o: WithComponent[T]): Option[(DecodeComponent, T)] = Some(o.component, o._2)
  }

  object ComponentCommand {
    def apply(component: DecodeComponent, command: DecodeCommand) = WithComponent[DecodeCommand](component, command)

    def unapply(o: WithComponent[DecodeCommand]) = WithComponent.unapply(o)
  }

  object ComponentMessage {
    def apply(component: DecodeComponent, message: DecodeMessage) = WithComponent[DecodeMessage](component, message)

    def unapply(o: WithComponent[DecodeMessage]) = WithComponent.unapply(o)
  }

  implicit class RichParameter(val parameter: DecodeMessageParameter) {
    def varName: String = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL,
      parameter.value.replaceAll("[\\.\\[\\]]", "_").replaceAll("__", "_"))
  }

  implicit class RichAstElement(val el: CAstElement) {
    def line: CStatementLine = CStatementLine(el)

    def eol: CAstElements = Seq(el, CEol)
  }

  implicit class RichCType(val ct: CType) {
    def const: CConstType = CConstType(ct)
  }

  private var fileNameId: Int = 0
  private var typeNameId: Int = 0

  implicit class RichOptionNamed(val optionNamed: DecodeOptionNamed) {
    def fileName: String =
      optionNamed.optionName.map(_.asMangledString).getOrElse { fileNameId += 1; "type" + fileNameId }

    def cTypeName: String =
      optionNamed.optionName.map(_.asMangledString).getOrElse { typeNameId += 1; "type" + typeNameId }
  }

  implicit class RichFile(val file: io.File) {
    def write(contents: CAstElements) {
      for (typeHeaderStream <- managed(new OutputStreamWriter(new FileOutputStream(file)))) {
        contents.generate(CGenState(typeHeaderStream))
      }
    }

    def writeIfNotEmptyWithComment(contents: CAstElements, comment: String) {
      if (contents.nonEmpty)
        file.write(CComment(comment).eol ++ contents)
    }
  }

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

  private val tag = TypedVar("tag", berType)
  private val size = TypedVar("size", berType)
  private val i = TypedVar("i", sizeTType)
  private val reader = TypedVar("reader", CTypeApplication("PhotonReader").ptr)
  private val writer = TypedVar("writer", CTypeApplication("PhotonWriter").ptr)
  private val componentId = TypedVar("componentId", sizeTType)
  private val commandId = TypedVar("commandId", sizeTType)
  private val messageId = TypedVar("messageId", sizeTType)

  private def upperCamelCaseToLowerCamelCase(str: String) = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, str)

  private def allComponentsSetForComponent(component: DecodeComponent,
                                           componentsSet: immutable.HashSet[DecodeComponent] = immutable.HashSet.empty)
      : immutable.HashSet[DecodeComponent] = {
    componentsSet ++ Seq(component) ++ component.subComponents.flatMap(cr =>
      allComponentsSetForComponent(cr.component.obj, componentsSet))
  }

  private def lowerUnderscoreToUpperCamel(str: String) = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, str)

  private def primitiveTypeToCTypeApplication(primitiveType: DecodePrimitiveType): CTypeApplication = {
    import TypeKind._
    (primitiveType.kind, primitiveType.bitLength) match {
      case (Bool, 8) => CUnsignedCharType
      case (Bool, 16) => CUnsignedShortType
      case (Bool, 32) => CUnsignedIntType
      case (Bool, 64) => CUnsignedLongType
      case (Float, 32) => CFloatType
      case (Float, 64) => CDoubleType
      case (Int, 8) => CSignedCharType
      case (Int, 16) => CSignedShortType
      case (Int, 32) => CSignedIntType
      case (Int, 64) => CSignedLongType
      case (Uint, 8) => CUnsignedCharType
      case (Uint, 16) => CUnsignedShortType
      case (Uint, 32) => CUnsignedIntType
      case (Uint, 64) => CUnsignedLongType
      case _ => sys.error("illegal bit length")
    }
  }

  private def upperCamelCaseToUpperUnderscore(s: String) = CaseFormat.UPPER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, s)

  private def isAliasNameTheSame(t: DecodeAliasType): Boolean = t.cTypeName == t.baseType.obj.cTypeName

  private val keywords = Seq("return")

  private def casesForMap[T <: DecodeNamed](map: immutable.HashMap[Int, WithComponent[T]],
                                            apply: (T, DecodeComponent) => CAstElements): Seq[CCase] =
    map.toSeq.sortBy(_._1).map { case (id, WithComponent(c, _2)) => CCase(CIntLiteral(id), apply(_2, c)) }
}
