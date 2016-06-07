package ru.mipt.acsl.decode.c.generator

import java.io._
import java.nio.charset.StandardCharsets
import java.{io, util}

import scala.collection.JavaConversions._
import com.google.common.base.CaseFormat
import com.typesafe.scalalogging.LazyLogging
import org.apache.commons.compress.compressors.xz.{XZCompressorInputStream, XZCompressorOutputStream}
import org.apache.commons.compress.utils.IOUtils
import ru.mipt.acsl.decode.generator.json.{DecodeJsonGenerator, DecodeJsonGeneratorConfig}
import ru.mipt.acsl.decode.model.{MayHaveId, Parameter, StatusParameter}
import ru.mipt.acsl.decode.model.component.message.{EventMessage, StatusMessage, TmMessage}
import ru.mipt.acsl.decode.model.component.{Command, Component, StatusParameter}
import ru.mipt.acsl.decode.model.naming.{HasName, Namespace}
import ru.mipt.acsl.decode.model.types.{Alias, DecodeType, EnumType, GenericTypeSpecialized, NativeType, PrimitiveTypeInfo, StructField, StructType, SubType, TypeAlias, TypeKind}
import ru.mipt.acsl.generator.c.ast.{CDefine, _}
import ru.mipt.acsl.generator.c.ast.implicits._

import scala.collection.immutable.HashMap
import scala.collection.{immutable, mutable}
import scala.io.Source

class CSourceGenerator(val config: CGeneratorConfiguration) extends LazyLogging {

  import CSourceGenerator._

  def generate(): Unit = {
    val component = config.registry.component(config.rootComponentFqn).getOrElse(
      sys.error(s"component not found ${config.rootComponentFqn}"))
    generateRoot(component)
  }

  private def provideSources(): Unit = {
    if (config.sources.isEmpty)
      return
    val dir = new File(config.outputDir, "decode/")
    dir.mkdirs()
    config.sources.foreach(s => write(new File(dir, new File(s.name).getName), s.contents))
  }

  private def generatePrologueEpilogue(): Unit = {
    generateFile(config.prologue.isActive, prologuePath, config.prologue.contents)
    generateFile(config.epilogue.isActive, epiloguePath, config.epilogue.contents)
  }

  private def generateFile(isActive: Boolean, filePath: String, contents: Option[String]): Unit = {
    if (isActive) for (c <- contents) {
      val f = new io.File(config.outputDir, filePath)
      f.getParentFile.mkdirs()
      write(f, c)
    }
  }

  private def prologuePath: String = config.prologue.path.getOrElse("photon_prologue.h")
  private def epiloguePath: String = config.epilogue.path.getOrElse("photon_epilogue.h")

  private def appendPrologueEpilogue(file: CAstElements): CAstElements = {
    (if (config.prologue.isActive)
      Seq(CInclude(prologuePath), CEol, CEol)
    else
      CAstElements()) ++
      file ++
      (if (config.epilogue.isActive)
        Seq(CEol, CInclude(epiloguePath), CEol, CEol)
      else
        CAstElements())
  }

  private def isAliasNameAndRawTypeNameAreEquals(t: TypeAlias[_, _ <: DecodeType]): Boolean = cTypeName(t) == cTypeName(t.obj)

  private def generate(t: DecodeType, nsDir: io.File): (CAstElements, CAstElements) = {
    var (h, c): (CAstElements, CAstElements) = t match {
      case t: NativeType if t.isPrimitive => (Seq(cTypeDef(t, cType(t))), Seq.empty)
      case t: NativeType => (Seq(cTypeDef(t, CVoidType.ptr)), Seq.empty)
      case t: SubType => (Seq(cTypeDef(t, cType(t.baseType))), Seq.empty)
      case t: EnumType =>
        val prefixedEnumName = upperCamel2UpperUnderscore(prefixedCTypeName(t))
        (Seq(cTypeDef(t, CEnumTypeDef(t.allConstants.toSeq.sortBy(_.value.toLongOrFail).map(c =>
          CEnumTypeDefConst(prefixedEnumName + "_" + c.name.mangledNameString(), c.value.toLongOrFail))))),
          Seq.empty)
      case t: GenericTypeSpecialized => sys.error("not implemented")/*t.genericType match {
          case optional if optional.name.equals(ElementName.newFromMangledName("optional")) =>
            require(t.genericTypeArguments.size == 1)
            val head = t.genericTypeArguments.head
            head match {
              // TODO: implement or remove
              // case h if h.isBasedOnEnum => (Seq(t.cTypeDef(head.obj.cType)), Seq.empty)
              case _ => (Seq(t.cTypeDef(CStructTypeDef(Seq(
                CStructTypeDefField("value", t.genericTypeArguments.head.cType),
                CStructTypeDefField("flag", b8Type))))), Seq.empty)
            }
          case or if or.name.equals(ElementName.newFromMangledName("or")) =>
            var index = 0
            (Seq(t.cTypeDef(CStructTypeDef(t.genericTypeArguments.map { t =>
              index += 1
              CStructTypeDefField("_" + index, t.cType)
            } :+ tag.field))), Seq.empty)
        }*/
      case t if t.isArray =>
        sys.error("not implemented")
      /*val arrayType: CType = t.size.max match {
        case 0 => t.baseType.cType.ptr
        case _ => CArrayType(t.baseType.cType, t.size.max, dataVar.name)
      }
      val typeDef = CTypeDefStatement(t.prefixedCTypeName, CStructTypeDef(Seq(
        CStructTypeDefField(size.name, sizeTType),
        CStructTypeDefField(dataVar.name, arrayType))))
      val defineNamePrefix = t.prefixedCTypeName.upperCamel2UpperUnderscore
      (Seq(CDefine(defineNamePrefix + "_MIN_SIZE", t.size.min.toString), CEol,
        CDefine(defineNamePrefix + "_MAX_SIZE", t.size.max.toString), CEol, CEol, typeDef), CAstElements())*/
      case t: StructType => (Seq(cTypeDef(t, CStructTypeDef(t.fields.map(f =>
        CStructTypeDefField(f.name.mangledNameString(), cType(f.typeMeasure.t)))))), Seq.empty)
      case t: TypeAlias[_, _] =>
        if (isAliasNameAndRawTypeNameAreEquals(t))
          (Seq.empty, Seq.empty)
        else
          (Seq(cTypeDef(t, cType(t.obj))), Seq.empty)
      case _ => sys.error(s"not implemented $t")
    }

    if (h.nonEmpty) {
      val _importTypes = importTypes(t)
      val imports = CAstElements(_importTypes.filterNot(_.isNative).flatMap(t => Seq(CInclude(relPath(t)), CEol)): _*)

      val selfType = cType(t)
      val serializeMethod = CFuncImpl(CFuncDef(serializeMethodName(t), resultType,
        Seq(CFuncParam(selfVar.name, mapIfNotSmall(selfType, t, (ct: CType) => ct.constPtr)), writer.param)),
        serializeCode(t) :+ CStatementLine(CReturn(resultOk)))
      val deserializeMethod = CFuncImpl(CFuncDef(deserializeMethodName(t), resultType,
        Seq(CFuncParam(selfVar.name, selfType.ptr), reader.param)), deserializeCode(t) :+ CStatementLine(CReturn(resultOk)))

      h = h ++ Seq(CEol, serializeMethod.definition, CEol, CEol, deserializeMethod.definition)
      c = c ++ Seq(CEol, serializeMethod, CEol, CEol, deserializeMethod)

      ((if (imports.nonEmpty) imports :+ CEol else imports) ++ externC(h) ++ Seq(CEol), c)
    } else {
      (CAstElements(), CAstElements())
    }
  }

  def relPath(t: DecodeType): String = dirPath(t.namespace) + io.File.separator + fileName(t) + headerExt

  def generate(ns: Namespace): Unit = {
    val nsDir = ensureDirExists(ns)
    ns.subNamespaces.foreach(generate)
    val typesHeader = CAstElements()
    ns.types.foreach(t => generateSeparateFiles(t, nsDir))
    val fileName: String = "types" + headerExt
    writeIfNotEmptyWithComment(new io.File(nsDir, fileName), protectDoubleInclude(typesHeader, dirPath(ns) +
      io.File.separator + fileName), s"Types of ${ns.fqn.mangledNameString()} namespace")
  }


  def generateSeparateFiles(t: DecodeType, nsDir: io.File): Unit = if (!t.isNative) {
    val _fileName = fileName(t)
    val hFileName = _fileName + headerExt
    val cFileName = _fileName + sourcesExt
    val (hFile, cFile) = (new io.File(nsDir, hFileName), new io.File(nsDir, cFileName))
    val (h, c) = generate(t, nsDir)
    if (h.nonEmpty)
      writeIfNotEmptyWithComment(hFile,
        protectDoubleInclude(Seq(CEol) ++ appendPrologueEpilogue(h) ++ Seq(CEol), relPath(t)),
        "Type header")
    else
      logger.debug(s"Omitting type $t")
    if (c.nonEmpty)
      writeIfNotEmptyWithComment(cFile, Seq(CInclude(relPath(t)), CEol, CEol) ++ c, "Type implementation")
  }

  def nsOrAliasCppSourceParts(ns: Namespace): Seq[String] =
    config.namespaceAliases.getOrElse(ns.fqn, Some(ns.fqn)).map(_.getParts.map(_.mangledNameString())).getOrElse(Seq.empty)

  def dirPath(ns: Namespace): String = nsOrAliasCppSourceParts(ns).mkString(io.File.separator)

  def includePathFor(ns: Namespace, fileName: String): String = dirPath(ns) + io.File.separator + fileName

  def dir(ns: Namespace): io.File = new io.File(config.outputDir, dirPath(ns))

  def ensureDirExists(ns: Namespace): io.File = {
    val _dir = dir(ns)
    if (!(_dir.exists() || _dir.mkdirs()))
      sys.error(s"Can't create directory ${_dir.getAbsolutePath}")
    _dir
  }

  def includePath(component: Component): String =
    includePathFor(component.namespace, prefixedTypeName(component) + headerExt)

  def generateRoot(component: Component): Unit = {
    logger.debug(s"Generating component ${component.name.mangledNameString()}")
    provideSources()
    generatePrologueEpilogue()
    config.isSingleton match {
      case true =>
        generateSingleton(component)
      case _ =>
        sys.error("not implemented")
        val nsSet = mutable.HashSet.empty[Namespace]
        collectNamespaces(component, nsSet)
        nsSet.foreach(generate)
        allComponentsSetForComponent(component).foreach(generate)
    }
  }

  def generate(component: Component): Unit = {
    val _dir = dir(component.namespace)
    val componentStructName = prefixedTypeName(component)
    val hFileName = componentStructName + headerExt
    val (hFile, cFile) = (new io.File(_dir, hFileName), new io.File(_dir, componentStructName + sourcesExt))
    val imports = importStatements(component)
    val componentFunctionTableName = functionTableTypeName(component)
    val componentFunctionTableNameStruct = componentFunctionTableName + structNamePostfix
    val forwardFuncTableDecl = CForwardStructDecl(componentFunctionTableNameStruct)
    val componentTypeStructName = componentStructName + structNamePostfix
    val componentTypeForwardDecl = CForwardStructTypeDef(componentStructName, componentTypeStructName)
    val componentType = structType(component)

    val methods = allCommandsMethods(component) ++ allStatusMessageMethods(component) ++
      allMethods(component)

    val externedCFile = externC(Seq(forwardFuncTableDecl, CEol, CEol) ++ Seq(componentTypeForwardDecl, CEol, CEol) ++
      Seq(componentType) ++ Seq(CSemicolon, CEol) ++ methods.flatMap(m => Seq(m.impl.definition, CEol)))
    writeIfNotEmptyWithComment(hFile, protectDoubleInclude(CEol +: appendPrologueEpilogue(imports ++ externedCFile),
      dirPath(component.namespace) + hFileName),
      s"Component ${component.name.mangledNameString()} interface")
    writeIfNotEmptyWithComment(cFile, Seq(CInclude(includePath(component)), CEol, CEol) ++
      methods.flatMap(m => Seq(m.impl, CEol, CEol)), s"Component ${component.name.mangledNameString()} implementation")
  }

  def collectNamespaces(component: Component, nsSet: mutable.HashSet[Namespace]) {
    component.subComponents.foreach(c => collectNamespaces(c.obj.obj, nsSet))
    collectNsForTypes(component, nsSet)
  }

  def collectNsForTypes(component: Component, set: mutable.Set[Namespace]) {
    for (baseType <- component.baseType)
      collectNsForType(baseType, set)
    component.commands.foreach { cmd =>
      cmd.parameters.foreach(arg => collectNsForType(arg.parameterType, set))
      collectNsForType(cmd.returnType, set)
    }
  }

  def generateSingleton(component: Component): Unit = {

    allTypes(component).foreach(t => generateSeparateFiles(t, ensureDirExists(t.namespace)))

    val nsDir = ensureDirExists(component.namespace)
    val componentStructName = prefixedTypeName(component)
    val (hFile, cFile) = (new File(nsDir, componentStructName + headerExt),
      new File(nsDir, componentStructName + sourcesExt))

    val guidDefines = componentDefines(component)

    val methods = allMethods(component) ++ allSubComponentsMethods(component)

    write(hFile, protectDoubleInclude(Seq(CEol) ++ appendPrologueEpilogue(externC(typeIncludes(component) ++
      Seq(CComment("USER command implementation functions, MUST BE implemented")) ++
      commandMethodImplDefs(component).groupBy(_.component).flatMap { case (c, cMethods) =>
        Seq(CComment("Component " + c.name.mangledNameString())) ++ cMethods.flatMap(m => Seq(CEol, m.methodDef)) ++ Seq(CEol)
      }.toSeq ++
      Seq(CComment("USER parameter implementation functions, MUST BE implemented")) ++
      parameterMethodImplDefs(component).groupBy(_.component).flatMap{ case (c, cMethods) =>
        Seq(CComment("Component " + c.name.mangledNameString())) ++ cMethods.flatMap(m => Seq(CEol, m.methodDef)) ++ Seq(CEol)
      }.toSeq ++
      Seq(CComment("USER service functions, MUST BE implemented")) ++
      serviceMethodDefs(component).flatMap(m => Seq(CEol, m)) ++ Seq(CEol) ++
      Seq(CComment("Component defines"), CEol) ++ guidDefines ++
      Seq(CComment("Message ID for component defines"), CEol) ++ allMessageDefines(component) ++
      Seq(CComment("Command ID for component defines"), CEol) ++ allCommandDefines(component) ++
      Seq(CComment("Public interface functions"), CEol) ++ methods.groupBy(_.component).flatMap {
      case (c, cMethods) => cMethods.filter(_.isPublic) match {
        case m if m.isEmpty => Seq.empty
        case m =>
          Seq(CEol) ++ Seq(CComment("Component " + c.name.mangledNameString())) ++
            m.flatMap(m => CAstElements(CEol, m.impl.definition))
      }
      }) ++ Seq(CEol)), dirPath(component.namespace) + hFile.getName))

    if (config.includeModelInfo) {
      val modelC = new StringBuilder()

      new ByteArrayOutputStream() {

        private val jsonConfig: DecodeJsonGeneratorConfig = DecodeJsonGeneratorConfig(config.registry, this, Seq(config.rootComponentFqn),
          prettyPrint = true)
        DecodeJsonGenerator(jsonConfig).generate()

        modelC.append("/*").append(new String(toByteArray, StandardCharsets.UTF_8)).append("*/\n\n")

        reset()

        DecodeJsonGenerator(jsonConfig.copy(prettyPrint = false)).generate()

        private val rawJsonMinified = toByteArray
        private val array = new ByteArrayOutputStream() {
          new XZCompressorOutputStream(this) {
            write(rawJsonMinified)
            finish()
            close()
          }
        }.toByteArray

        assert(util.Arrays.equals(rawJsonMinified,
          IOUtils.toByteArray(new XZCompressorInputStream(new ByteArrayInputStream(array)))))

        modelC.append(s"static uint8_t modelData[${array.length}] = {")
        for ((byte, index) <- array.zipWithIndex) {
          modelC.append("0x%02X, ".format(byte))
          if (index % 20 == 0)
            modelC.append("\n\t")
        }

        close()
      }
      modelC.append("};\n")

      write(new File(nsDir, "model" + sourcesExt), modelC.toString())
    }

    write(cFile, Seq(CInclude(includePathFor(component.namespace, hFile.getName)), CEol, CEol) ++
      methods.flatMap(m => Seq(m.impl.definition, CEol, CEol)) ++
      methods.flatMap(m => Seq(m.impl, CEol, CEol)))
  }

  def typeIncludes(component: Component): CAstElements =
    allTypes(component).toSeq.filter(t => isGeneratable(t)).flatMap(t => Seq(CInclude(relPath(t)), CEol))

  def componentDefines(component: Component): CAstElements = {
    Seq(CDefine("PHOTON_COMPONENTS_SIZE", allComponentsById(component).size.toString), CEol,
      CDefine("PHOTON_COMPONENT_IDS", '{' + allComponentsById(component).keys.toSeq.sorted.mkString(", ") + '}'), CEol) ++
      allComponentsById(component).flatMap { case (id, c) =>
        val _guidDefineName = guidDefineName(c)
        Seq(CDefine(_guidDefineName, '"' + c.fqn.mangledNameString() + '"'), CEol,
          CDefine("PHOTON_COMPONENT_" + id + "_GUID", _guidDefineName), CEol,
          CDefine(idDefineName(c), id.toString), CEol)
      } ++
      Seq(CDefine("PHOTON_COMPONENT_GUIDS", '{' + allComponentsById(component).toSeq.sortBy(_._1).map(_._2)
        .map(c => guidDefineName(c)).mkString(", ") + '}'), CEol)
  }

  def importStatements(component: Component): CAstElements = {
    val imports = component.subComponents.flatMap(cr => Seq(CInclude(includePath(cr.obj.obj)), CEol)).to[mutable.Buffer]
    if (imports.nonEmpty)
      imports += CEol
    val types = allTypes(component).toSeq
    val typeIncludes = types.filterNot(_.isNative).flatMap(t => Seq(CInclude(relPath(t)), CEol))
    imports ++= typeIncludes
    if (typeIncludes.nonEmpty)
      imports += CEol
    imports.to[immutable.Seq]
  }

}

private[generator] object CSourceGenerator {

  val PtrSize = 4
  val VaruintByteSize = 8

  val headerExt = ".h"
  val sourcesExt = ".c"
  val structNamePostfix = "_s"
  val cppDefine = "__cplusplus"
  val typePrefix = "PhotonGc"

  val tryMacroName = "PHOTON_TRY"
  val typeInitMethodName = "Init"

  val photonBerTypeName = "PhotonBer"
  val b8Type = CTypeApplication("PhotonGtB8")
  val berType = CTypeApplication("PhotonBer")
  val voidType = CTypeApplication("void")
  val sizeTType = CTypeApplication("size_t")

  val resultType = CTypeApplication("PhotonResult")
  val resultOk = CVar(resultType.name + "_Ok")

  val selfVar = CVar("self")
  val invalidMessageId = CVar("PhotonResult_InvalidMessageId")
  val invalidCommandId = CVar("PhotonResult_InvalidCommandId")
  val invalidComponentId = CVar("PhotonResult_InvalidComponentId")
  val eventIsDenied = CVar("PhotonResult_EventIsDenied")

  val reader = TypedVar("reader", CTypeApplication("PhotonReader").ptr)
  val writer = TypedVar("writer", CTypeApplication("PhotonWriter").ptr)
  private val tag = TypedVar("tag", berType)
  val size = TypedVar("size", berType)
  val i = TypedVar("i", berType)

  val componentId = TypedVar("componentId", berType)
  val commandId = TypedVar("commandId", berType)
  val messageId = TypedVar("messageId", berType)
  val eventId = TypedVar("eventId", berType)

  def write(file: File, contents: CAstElements) {
    val os = new OutputStreamWriter(new FileOutputStream(file))
    try {
      contents.generate(CGenState(os))
    } finally {
      os.close()
    }
  }

  def write(file: File, source: Source): Unit = write(file, source.mkString)

  def write(file: File, contents: String): Unit = {
    val os = new OutputStreamWriter(new FileOutputStream(file))
    try {
      os.write(contents)
    } finally {
      os.close()
    }
  }

  def cType(t: DecodeType): CType = CTypeApplication(prefixedCTypeName(t))

  def cTypeDef(t: DecodeType, cType: CType) = CTypeDefStatement(prefixedCTypeName(t), cType)

  def cTypeName(a: TypeAlias[_, _]): String = a.name.mangledNameString()

  def cTypeName(t: DecodeType): String = t match {
    case t: GenericTypeSpecialized if t.isArray =>
      sys.error("not implemented")
    /*val baseCType = t.baseType.cTypeName
    val min = t.size.min
    val max = t.size.max
    "Arr" + baseCType + ((t.isFixedSize, min, max) match {
      case (true, 0, _) | (false, 0, 0) => ""
      case (true, _, _) => s"Fixed$min"
      case (false, 0, _) => s"Max$max"
      case (false, _, 0) => s"Min$min"
      case (false, _, _) => s"Min${min}Max$max"
    })*/
    case t: GenericTypeSpecialized =>
      cTypeName(t.genericType) +
        t.genericTypeArguments.map(cTypeName).mkString
    case named => sys.error("not implemented")// named.name.asMangledString.lowerUnderscore2UpperCamel
  }

  def importTypes(t : DecodeType): Seq[DecodeType] = t match {
    case t: StructType => t.fields.flatMap { f =>
      val t = f.typeMeasure.t
      if (t.isNative)
        Seq.empty
      else
        Seq(t)
    }
    case s: GenericTypeSpecialized =>
      s.genericType match {
        case optional if optional.isOptionType =>
          Seq(s.genericTypeArguments.head)
        case or if or.isOrType =>
          s.genericTypeArguments
      }
    /*case t: HasBaseType =>
    if (t.baseType.isNative)
      Seq.empty
    else
      Seq(t.baseType)*/
    case _ => Seq.empty
  }

  def prefixedCTypeName(t: DecodeType): String = "PhotonGt" + cTypeName(t)

  def fileName(t: DecodeType): String = prefixedCTypeName(t)

  def upperCamel2LowerCamel(str: String): String = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, str)

  def upperCamel2UpperUnderscore(str: String): String = CaseFormat.UPPER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, str)

  def lowerUnderscore2UpperCamel(str: String): String = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, str)

  def writeIfNotEmptyWithComment(file: File, contents: CAstElements, comment: String) {
    if (contents.nonEmpty)
      write(file, Seq(CComment(comment), CEol) ++ contents)
  }

  def protectDoubleInclude(els: CAstElements, filePath: String): CAstElements = {
    val uniqueName = "__" + filePath.split(io.File.separatorChar).map(p =>
      upperCamel2UpperUnderscore(p).replaceAll("\\.", "_")).mkString("_") + "__"
    Seq(CComment("DO NOT EDIT! FILE IS AUTO GENERATED"), CEol) ++
      Seq(CIfNDef(uniqueName), CEol, CDefine(uniqueName)) ++ els :+ CEndIf
  }

  def typeName(component: Component): String = component.name.mangledNameString()

  def ptrType(component: Component): CPtrType = CTypeApplication(prefixedTypeName(component)).ptr

  def prefixedTypeName(c: Component): String = typePrefix + typeName(c)

  def externC(els: CAstElements): CAstElements =
    Seq(CIfDef(cppDefine), CEol, CPlainText("extern \"C\" {"), CEol, CEndIf, CEol, CEol) ++ els ++
      Seq(CEol, CEol, CIfDef(cppDefine), CEol, CPlainText("}"), CEol, CEndIf)

  def tryCall(methodName: String, exprs: CExpression*): CFuncCall = cTry(cCall(methodName, exprs: _*))

  def collectNsForType[T <: DecodeType](t: T, set: mutable.Set[Namespace]): Unit =
    collectNamespaces(t, set)

  def mapIfSmall[A <: B, B <: CAstElement](el: A, t: DecodeType, f: A => B): B = if (isSmall(t)) f(el) else el

  def mapIfNotSmall[A <: B, B <: CAstElement](el: A, t: DecodeType, f: A => B): B = if (isSmall(t)) el else f(el)

  private def allComponentsSetForComponent(component: Component,
                                           componentsSet: immutable.HashSet[Component] = immutable.HashSet.empty)
  : immutable.HashSet[Component] = {
    componentsSet ++ Seq(component) ++ component.subComponents.flatMap(cr =>
      allComponentsSetForComponent(cr.obj.obj, componentsSet))
  }

  def componentDataTypeName(component: Component): String = prefixedTypeName(component) + "Data"

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

  val keywords = Seq("return")

  def casesForMap[T <: HasName](map: immutable.HashMap[Int, WithComponent[T]],
                                        apply: (T, Component) => Option[CAstElements]): Seq[CCase] =
    map.toSeq.sortBy(_._1).flatMap { case (id, WithComponent(c, _2)) => apply(_2, c).map(els => CCase(CIntLiteral(id), els)) }

  def dotOrArrow(t: DecodeType, expr: CExpression, exprRight: CExpression): CExpression = isSmall(t) match {
    case true => cDot(expr, exprRight)
    case _ => ->(expr, exprRight)
  }

  def methodName(t: DecodeType, name: String): String = methodName(prefixedCTypeName(t), name)

  def cMethodReturnType(t: DecodeType): CType = if (isSmall(t)) cType(t) else cType(t).constPtr

  def cMethodReturnParameters(t: DecodeType): Seq[CFuncParam] = Seq.empty //if (t.isSmall) Seq.empty else Seq(CFuncParam("result", t.cType.ptr))

  def isBasedOnEnum(t: DecodeType): Boolean = t match {
    case _: EnumType => true
    case t: GenericTypeSpecialized if t.isArray => false
    //case t: HasBaseType => t.baseType.isBasedOnEnum
    case _ => false
  }

  def collectNamespaces(t: DecodeType, set: mutable.Set[Namespace]) {
    set += t.namespace
    t match {
      //case t: HasBaseType => collectNsForType(t.baseType, set)
      case t: StructType => t.fields.foreach(f => collectNsForType(f.typeMeasure.t, set))
      case t: GenericTypeSpecialized => t.genericTypeArguments.foreach(collectNsForType(_, set))
      case _ =>
    }
  }

  def isSmall(t: DecodeType): Boolean = byteSize(t) <= 16

  def refIfNotSmall(expr: CExpression, t: DecodeType): CExpression = if (isSmall(t)) expr else ref(expr)

  def mapIf(expr: CExpression, flag: Boolean, mapper: CExpression => CExpression): CExpression = if (flag) mapper(expr) else expr

  def derefIf(expr: CExpression, flag: Boolean): CExpression = mapIf(expr, flag, deref)

  def derefIfSmall(expr: CExpression, t: DecodeType): CExpression = derefIf(expr, isSmall(t))

  def assign(expr: CExpression, right: CExpression) = CAssign(expr, right)

  def cDot(expr: CExpression, right: CExpression): CDot = CDot(expr, right)

  def dotOrArrow(expr: CExpression, expr2: CExpression, isDot: Boolean): CExpression =
    if (isDot) cDot(expr, expr2) else ->(expr, expr2)

  def deref(expr: CExpression) = CDeref(expr)

  def cast(expr: CExpression, cType: CType): CTypeCast = CTypeCast(expr, cType)

  def byteSize(t: DecodeType): Int = byteSize(t, Set.empty)

  def byteSize(t: DecodeType, enclosingTypes: Set[DecodeType]): Int = enclosingTypes.contains(t) match {
    case true => PtrSize
    case _ =>
      val extendedEclosingTypes = enclosingTypes + t
      t match {
        case t: NativeType if t.isPrimitive => (t.primitiveTypeInfo.bitLength / 8).toInt
        case t: GenericTypeSpecialized if t.isArray =>
          sys.error("not implemented")
        /*t.size.max match {
        case 0 => VaruintByteSize + PtrSize
        case _ => (t.size.max * t.baseType.byteSize(extendedEnclosingTypes)).toInt
      }*/
        case t: GenericTypeSpecialized => t.genericType match {
          case optional if optional.isOptionType => 1 + byteSize(t.genericTypeArguments.head, extendedEclosingTypes)
          case or if or.isOrType => 1 + t.genericTypeArguments.map(a => byteSize(a, extendedEclosingTypes)).max
          case _ => sys.error(s"not implemented for $t")
        }
        case n: NativeType => n.isVaruintType match {
          case true => VaruintByteSize
          case _ => sys.error(s"not implemented for $n")
        }
        case t: StructType => t.fields.map(f => byteSize(f.typeMeasure.t, extendedEclosingTypes)).sum
        //case t: HasBaseType => t.baseType.byteSize(extendedEnclosingTypes)
        case _ => sys.error(s"not implemented for $t")
      }
  }

  def cCall(str: String, exprs: CExpression*) = CFuncCall(str, exprs: _*)

  def methodName(str: String, name: String): String = str + "_" + name.capitalize

  def initMethodName(str: String): String = methodName(str, typeInitMethodName)

  def comment(str: String): CAstElements = Seq(CEol, CComment(str), CEol)

  def executeCommandMethodNamePart: String = "ExecuteCommand"

  def cTry(expr: CExpression): CFuncCall = cCall(tryMacroName, expr)

  def ->(expr: CExpression, expr2: CExpression): CArrow = CArrow(expr, expr2)

  def apply(expr: CExpression, indexExpr: CExpression): CIndex = CIndex(expr, indexExpr)

  def functionForCommandMethodName(component: Component): String =
    methodName(prefixedTypeName(component), "FunctionForCommand")

  def executeCommandMethodName(component: Component, rootComponent: Component): String =
    if (component == rootComponent)
      methodName(prefixedTypeName(component), executeCommandMethodNamePart)
    else
      methodName(prefixedTypeName(rootComponent), cName(component) + executeCommandMethodNamePart)

  def readExecuteCommandMethodNamePart: String = "ReadExecuteCommand"

  def readExecuteCommandMethodName(component: Component, rootComponent: Component): String =
    if (component == rootComponent)
      methodName(prefixedTypeName(component), readExecuteCommandMethodNamePart)
    else
      methodName(prefixedTypeName(rootComponent), cName(component) + readExecuteCommandMethodNamePart)

  def writeStatusMessageMethodName(component: Component, rootComponent: Component): String =
    if (component == rootComponent)
      methodName(prefixedTypeName(component), "WriteStatusMessage")
    else
      methodName(prefixedTypeName(rootComponent), cName(component) + "WriteMessage")

  def isEventAllowedMethodName(component: Component): String = methodName(prefixedTypeName(component), "IsEventAllowed")

  def isStatusMessageMethodName(component: Component): String = methodName(prefixedTypeName(component), "IsStatusMessage")

  def functionTableTypeName(component: Component): String = prefixedTypeName(component) + "UserFunctionTable"

  def guidDefineName(component: Component): String = upperCamel2UpperUnderscore(prefixedTypeName(component)) + "_GUID"

  def idDefineName(component: Component): String = upperCamel2UpperUnderscore(prefixedTypeName(component)) + "_ID"

  def executeMethodNamePart(component: Component, c: Component): String =
    "Execute" + methodNamePart(component, c).capitalize

  def executeMethodNamePart(component: Component, command: Command, c: Component): String =
    "Execute" + methodNamePart(component, command, c).capitalize

  def executeMethodName(component: Component, c: Component): String =
    methodName(prefixedTypeName(component), executeMethodNamePart(component, c))

  def executeMethodName(component: Component, command: Command, c: Component): String =
    methodName(prefixedTypeName(component), executeMethodNamePart(component, command, c))

  def methodNamePart(component: Component, c: Component): String =
    upperCamel2LowerCamel((if (component == c) "" else typeName(c)) +
      cName(component).capitalize)

  def methodNamePart(component: Component, part: String, c: Component): String =
    upperCamel2LowerCamel((if (component == c) "" else typeName(c)) + part)

  def methodNamePart(component: Component, command: Command, c: Component): String =
    methodNamePart(component, cName(command).capitalize, c)

  def methodNamePart(component: Component, message: TmMessage, c: Component): String =
    methodNamePart(component, cName(message).capitalize, c)

  def methodNamePart(component: Component, field: StructField, c: Component): String =
    methodNamePart(component, cName(field).capitalize, c)

  def methodName(component: Component, part: String): String = methodName(prefixedTypeName(component), part)

  def methodName(component: Component, c: Component): String = methodName(component, methodNamePart(component, c))

  def methodName(component: Component, command: Command, c: Component): String =
    methodName(component, methodNamePart(component, command, c))

  def methodName(component: Component, field: StructField, c: Component): String =
    methodName(component, methodNamePart(component, field, c))

  def beginNewEventMethodName(component: Component): String = methodName(prefixedTypeName(component), "BeginNewEvent")

  def endEventMethodName(component: Component): String = methodName(prefixedTypeName(component), "EndEvent")

  def writeStatusMessageMethod(component: Component): CFuncImpl = writeStatusMessageMethod(component)

  def writeStatusMessageMethod(component: Component, rootComponent: Component): CFuncImpl = {
    CFuncImpl(CFuncDef(writeStatusMessageMethodName(component, rootComponent), resultType,
      Seq(writer.param, messageId.param)),
      Seq(CStatementLine(cTry(serializeBer(messageId.v))), CIndent, CSwitch(messageId.v,
        casesForMap(allStatusMessagesById(component), { (message: TmMessage, c: Component) => message match {
          case message: StatusMessage =>
            Some(CStatements(CReturn(cCall(fullImplMethodName(message, rootComponent, c), writer.v))))
          case _ => None
        }
        }),
        default = CStatements(CReturn(invalidMessageId))), CEol))
  }

  def fullImplMethodName(message: TmMessage, rootComponent: Component, component: Component): String =
    methodName(prefixedTypeName(rootComponent),
      "Write" + methodNamePart(rootComponent, message, component).capitalize + "Impl")

  def fullMethodName(message: TmMessage, rootComponent: Component, component: Component): String =
    methodName(prefixedTypeName(rootComponent), "Write" + methodNamePart(rootComponent, message, component).capitalize)

  def executeCommandMethod(component: Component): CFuncImpl = executeCommandMethod(component)

  def executeCommandMethod(component: Component, rootComponent: Component): CFuncImpl = {
    CFuncImpl(CFuncDef(executeCommandMethodName(component, rootComponent), resultType,
      Seq(reader.param, writer.param, commandId.param)),
      Seq(CIndent, CSwitch(commandId.v, casesForMap(allCommandsById(component),
        (command: Command, c: Component) =>
          Some(CStatements(CReturn(cCall(executeMethodName(rootComponent, command, c), reader.v, writer.v))))),
        default = CStatements(CReturn(invalidCommandId))), CEol))
  }

  def readExecuteCommandMethod(component: Component): CFuncImpl = readExecuteCommandMethod(component)

  def readExecuteCommandMethod(component: Component, rootComponent: Component): CFuncImpl = {
    CFuncImpl(CFuncDef(readExecuteCommandMethodName(component, rootComponent), resultType,
      Seq(reader.param, writer.param)),
      CStatements(cDefine(commandId.v, commandId.t),
        tryCall(methodName(photonBerTypeName, typeDeserializeMethodName), ref(commandId.v), reader.v),
        CReturn(CFuncCall(executeCommandMethodName(component, rootComponent), reader.v, writer.v, commandId.v))))
  }

  def executeCommandForComponentMethodNamePart: String = "ExecuteCommandForComponent"

  def executeCommandForComponentMethodName(component: Component, rootComponent: Component): String =
    if (component == rootComponent)
      methodName(prefixedTypeName(component), executeCommandForComponentMethodNamePart)
    else
      methodName(prefixedTypeName(rootComponent), executeCommandForComponentMethodNamePart + cName(component))

  def executeCommandForComponentMethod(component: Component, rootComponent: Component): CFuncImpl = {
    CFuncImpl(CFuncDef(executeCommandForComponentMethodName(component, rootComponent), resultType,
      Seq(reader.param, writer.param, commandId.param)),
      Seq(CIndent, CSwitch(commandId.v, allComponentsById(component).toSeq.sortBy(_._1).map { case (id, c) =>
        CCase(CIntLiteral(id), CStatements(CReturn(cCall(readExecuteCommandMethodName(c, rootComponent),
          reader.v, writer.v))))
      }, default = CStatements(CReturn(invalidCommandId))), CEol))
  }

  def executeCommandForComponentMethod(component: Component): CFuncImpl = {
    CFuncImpl(CFuncDef(executeCommandForComponentMethodName(component, component), resultType,
      Seq(reader.param, writer.param, componentId.param, commandId.param)),
      Seq(CIndent, CSwitch(componentId.v, allComponentsById(component).toSeq.sortBy(_._1).map { case (id, c) =>
        CCase(CIntLiteral(id), CStatements(CReturn(
          cCall(if (c == component)
            executeCommandMethodName(c, component)
          else
            executeCommandForComponentMethodName(c, component),
            reader.v, writer.v, commandId.v))))
      }, default = CStatements(CReturn(invalidComponentId))), CEol))
  }

  def allMethods(component: Component): Seq[MethodInfo] = allCommandsMethods(component) ++
    allStatusMessageMethods(component) ++ allEventMessageMethods(component) ++
    Seq(MethodInfo(executeCommandMethod(component), component),
      MethodInfo(readExecuteCommandMethod(component), component, isPublic = true),
      MethodInfo(writeStatusMessageMethod(component), component, isPublic = true),
      MethodInfo(executeCommandForComponentMethod(component), component)) ++
    allSubComponents(component).map(c => MethodInfo(executeCommandForComponentMethod(c, component), c))

  def parameterMethodName(component: Component, parameter: StatusParameter, rootComponent: Component): String =
    methodName(prefixedTypeName(rootComponent), cStructFieldName(parameter.ref(component).structField
      .orElse {
        sys.error("not implemented")
      }, rootComponent, component))

  def allEventMessageMethods(component: Component): Seq[MethodInfo] = {
    allEventMessagesById(component).toSeq.sortBy(_._1).flatMap {
      case (id, ComponentEventMessage(c, eventMessage)) =>
        val eventVar = CVar("event")
        val methodName = fullImplMethodName(eventMessage, component, c)
        val _fullMethodDef = fullMethodDef(eventMessage, component, c)
        val eventParams = _fullMethodDef.parameters
        val eventParam = eventParams.head
        val idLiteral = CIntLiteral(id)
        Seq(MethodInfo(CFuncImpl(CFuncDef(methodName, resultType, writer.param +: eventParams),
          CAstElements(CIndent, CIf(CEq(CIntLiteral(0),
            cCall(isEventAllowedMethodName(component), idLiteral, CTypeCast(eventVar, berType))),
            CAstElements(CEol, CIndent, CReturn(eventIsDenied), CSemicolon, CEol)),
            CStatementLine(serializeCallCode(eventMessage.baseType, eventVar))) ++
            eventMessage.parameters.flatMap {
              case s: StatusParameter =>
                serializeCallCode(s, component, c)
              case p: Parameter =>
                CStatements(serializeCallCode(p.parameterType, CVar(cName(p))))
            } :+ CStatementLine(CReturn(resultOk))
        ), c),
          MethodInfo(CFuncImpl(_fullMethodDef,
            CStatements(
              cDefine(writer.v, writer.t, Some(cCall(beginNewEventMethodName(component), idLiteral, CVar(eventParam.name)))),
              cTry(cCall(methodName, writer.v +: eventParams.map(p => CVar(p.name)): _*)),
              CReturn(cCall(endEventMethodName(component))))), c, isPublic = true))
    }
  }

  def cDefine(v: CVar, t: CType, init: Option[CExpression] = None, static: Boolean = false) =
    CVarDef(v.name, t, init, static)

  def allStatusMessageMethods(component: Component): Seq[MethodInfo] = {
    allStatusMessagesById(component).toSeq.sortBy(_._1).map(_._2).map {
      case ComponentStatusMessage(c, statusMessage) =>
        MethodInfo(CFuncImpl(CFuncDef(fullImplMethodName(statusMessage, component, c), resultType,
          Seq(writer.param)),
          statusMessage.parameters.flatMap(p =>
            CStatementLine(CComment(p.toString)) +: serializeCallCode(p, component, c)) :+
            CStatementLine(CReturn(resultOk))), c)
    }
  }

  def allCommandDefines(component: Component): CAstElements = allCommandDefines(component)

  def allCommandDefines(component: Component, rootComponent: Component): CAstElements =
    commandDefines(component, rootComponent) ++
      allSubComponents(component).toSeq.flatMap(c => commandDefines(c, component))

  def commandDefines(component: Component, rootComponent: Component): CAstElements = {
    val defineName: String = upperCamel2UpperUnderscore(prefixedTypeName(rootComponent) +
      (if (rootComponent == component) "" else cName(component)) + "CommandIds")
    val commandById = allCommandsById(component)
    Seq(CDefine(defineName + "_LEN", commandById.size.toString), CEol) ++
      Seq(CDefine(defineName, "{" + commandById.toSeq.map(_._1).sorted.mkString(", ") + "}"), CEol)
  }

  def allMessageDefines(component: Component): CAstElements = allMessageDefines(component)

  def allMessageDefines(component: Component, rootComponent: Component): CAstElements =
    messageDefines(component, rootComponent) ++
      allSubComponents(component).toSeq.flatMap(c => messageDefines(c, component))

  def messageDefines(component: Component, rootComponent: Component): CAstElements = {
    val prefix = upperCamel2UpperUnderscore(prefixedTypeName(rootComponent) +
      (if (rootComponent == component) "" else cName(component)))
    val eventIdsDefineName = prefix + "_EVENT_MESSAGE_IDS"
    val statusIdsDefineName = prefix + "_STATUS_MESSAGE_IDS"
    val statusMessageIdPrioritiesDefineName = prefix + "_STATUS_MESSAGE_ID_PRIORITIES"
    val statusPrioritiesDefineName = prefix + "_STATUS_MESSAGE_PRIORITIES"
    val statusMessageById = allStatusMessagesById(component)
    val statusMessagesSortedById = statusMessageById.toSeq.sortBy(_._1)
    val eventMessageById = allEventMessagesById(component)
    val eventMessagesSortedById = eventMessageById.toSeq.sortBy(_._1)
    Seq(CDefine(eventIdsDefineName + "_SIZE", eventMessageById.size.toString), CEol) ++
      Seq(CDefine(eventIdsDefineName, "{" + eventMessagesSortedById.map(_._1).mkString(", ") + "}"), CEol) ++
      Seq(CDefine(statusIdsDefineName + "_SIZE", statusMessagesSortedById.size.toString), CEol) ++
      Seq(CDefine(statusIdsDefineName, '{' + statusMessagesSortedById.map(_._1.toString).mkString(", ") + '}'), CEol) ++
      Seq(CDefine(statusPrioritiesDefineName,
        "{" + statusMessagesSortedById.map(s => Option(s._2._2.priority).getOrElse(0)).mkString(", ") + "}"), CEol) ++
      Seq(CDefine(statusMessageIdPrioritiesDefineName, "{\\\n" + statusMessagesSortedById.map {
        case (id, ComponentStatusMessage(c, m)) => s"  {$id, ${Option(m.priority).getOrElse(0)}}"
        case _ => sys.error("assertion error")
      }.mkString("\\\n") + "\\\n}"), CEol)
  }

  def parameterMethodImplDefs(component: Component): Seq[MethodDefInfo] = {
    val parameters: Seq[ComponentParameterField] = allParameters(component)
    val res = parameters.map { case ComponentParameterField(c, f) =>
      val fType = f.typeMeasure.t
      MethodDefInfo(CFuncDef(methodName(component, f, c), cMethodReturnType(fType), cMethodReturnParameters(fType)), c)
    }
    res
  }

  def beginNewEventMethodDef(component: Component): CFuncDef =
    CFuncDef(beginNewEventMethodName(component), writer.t, Seq(messageId.param, eventId.param))

  def endEventMethodDef(component: Component): CFuncDef = CFuncDef(endEventMethodName(component), resultType)

  def isEventAllowedMethodDef(component: Component): CFuncDef =
    CFuncDef(isEventAllowedMethodName(component), b8Type, Seq(messageId.param, eventId.param))

  def serviceMethodDefs(component: Component): Seq[CFuncDef] =
    Seq(beginNewEventMethodDef(component), endEventMethodDef(component), isEventAllowedMethodDef(component))


  def commandMethodImplDefs(component: Component): Seq[MethodDefInfo] = {
    allCommands(component).map { case ComponentCommand(c, command) =>
      MethodDefInfo(CFuncDef(methodName(component, command, c),
        cMethodReturnType(command.returnType),
        command.parameters.map(p => {
          val t = p.parameterType
          CFuncParam(cName(p), mapIfNotSmall(cType(t), t, (ct: CType) => ct.constPtr))
        }) ++ cMethodReturnParameters(command.returnType)), c)
    }
  }

  def allSubComponentsMethods(component: Component): Seq[MethodInfo] = {
    allSubComponents(component).toSeq.flatMap { subComponent =>
      Seq(executeCommandMethod(subComponent, component),
        readExecuteCommandMethod(subComponent, component),
        writeStatusMessageMethod(subComponent, component))
        .map(f => MethodInfo(f, subComponent))
    }
  }

  def allCommandsMethods(component: Component): Seq[MethodInfo] = {
    val componentTypeName = prefixedTypeName(component)
    val parameters = Seq(reader.param, writer.param)
    allCommandsById(component).toSeq.sortBy(_._1).map(_._2).map { case ComponentCommand(subComponent, command) =>
      val methodNamePart = executeMethodNamePart(component, command, subComponent)
      val vars = command.parameters.map(p => CVar(mangledCName(p)))
      val varInits = vars.zip(command.parameters).flatMap { case (v, parameter) =>
        val paramType = parameter.parameterType
        CStatements(cDefine(v, cType(paramType)), cTry(deserializeCallCode(paramType, ref(v))))
      }
      val cmdReturnType = command.returnType
      val funcCall = cCall(methodName(component, command, subComponent),
        (for ((v, t) <- vars.zip(command.parameters.map(_.parameterType))) yield refIfNotSmall(v, t)): _*)
      MethodInfo(CFuncImpl(CFuncDef(methodName(componentTypeName, methodNamePart), resultType, parameters),
        varInits ++ (cmdReturnType match {
          case n: NativeType if n.isPrimitive => CStatements(serializeCallCode(n, funcCall), CReturn(resultOk))
          case rt if rt.isUnit => CStatements(funcCall, CReturn(resultOk))
          case rt => CStatements(CReturn(serializeCallCode(rt, funcCall)))
        })), subComponent)
    }
  }

  // todo: optimize: memoize
  private def makeMapById[T <: MayHaveId](component: Component, seq: Seq[T], subSeq: Component => Seq[T])
  : immutable.HashMap[Int, WithComponent[T]] = {
    var nextId = 0
    val mapById = mutable.HashMap.empty[Int, WithComponent[T]]
    // fixme: remove Option.get
    seq.filter(_.id != null).foreach(el => assert(mapById.put(el.id, WithComponent[T](component, el)).isEmpty))
    seq.filter(_.id != null).foreach { el =>
      // todo: optimize: too many contain checks
      while (mapById.contains(nextId))
        nextId += 1
      assert(mapById.put(Option(el.id.toInt).getOrElse {
        nextId += 1
        nextId - 1
      }, WithComponent[T](component, el)).isEmpty)
    }
    allSubComponents(component).toSeq.sortBy(_.fqn.mangledNameString()).filterNot(_ == component).foreach(subComponent =>
      subSeq(subComponent).foreach { el =>
        assert(mapById.put(nextId, WithComponent[T](subComponent, el)).isEmpty)
        nextId += 1
      })
    immutable.HashMap(mapById.toSeq: _*)
  }

  def allCommandsById(component: Component): HashMap[Int, WithComponent[Command]] =
    makeMapById(component, component.commands, _.commands)

  def allStatusMessagesById(component: Component): HashMap[Int, WithComponent[StatusMessage]] =
    makeMapById(component, component.statusMessages, _.statusMessages)

  def allEventMessagesById(component: Component): HashMap[Int, WithComponent[EventMessage]] =
    makeMapById(component, component.eventMessages, _.eventMessages)

  def allComponentsById(component: Component): HashMap[Int, Component] = {
    val map = mutable.HashMap.empty[Int, Component]
    var nextId = 0
    val components = component +: allSubComponents(component).toSeq
    val (withId, withoutId) = (components.filter(_.id != null), components.filter(_.id == null))
    withId.foreach { c => assert(map.put(Option(c.id.toInt).getOrElse(sys.error("wtf")), c).isEmpty) }
    map ++= withoutId.map { c =>
      while (map.contains(nextId))
        nextId += 1
      nextId += 1
      (nextId - 1, c)
    }
    HashMap(map.toSeq: _*)
  }

  def allSubComponents(component: Component): Set[Component] =
    component.subComponents.flatMap { alias =>
      val c = alias.obj.obj
      val set = allSubComponents(c)
      set.add(c)
      set
    }.toSet

  def allCommands(component: Component): Seq[WithComponent[Command]] =
    allSubComponents(component).toSeq.flatMap(sc => sc.commands.map(ComponentCommand(sc, _)))

  def allParameters(component: Component): Seq[ComponentParameterField] =
    component.baseType.map(_.fields.map(ComponentParameterField(component, _))).getOrElse(Seq.empty) ++
      allSubComponents(component).toSeq.flatMap(sc => sc.baseType.map(_.fields.map(ComponentParameterField(sc, _)))
        .getOrElse(Seq.empty))

  def structType(component: Component): CStructTypeDef = {
    val componentSelfPtrType = ptrType(component)
    CStructTypeDef(Seq(CStructTypeDefField("data", CTypeApplication(componentDataTypeName(component)).ptr)) ++
      allCommands(component).map { case ComponentCommand(c, command) =>
        structTypeFieldForCommand(component, c, command)
      } ++
      allParameters(component).map { case ComponentParameterField(c, f) =>
        val name = cStructFieldName(f, component, c)
        CStructTypeDefField(
          name, CFuncType(cType(f.typeMeasure.t), Seq(componentSelfPtrType), name))
      } ++
      component.baseType.map(_.fields.map { f =>
        val name = mangledCName(f)
        CStructTypeDefField(name, CFuncType(cType(f.typeMeasure.t), Seq(componentSelfPtrType), name))
      }).getOrElse(Seq.empty) ++ component.commands.map(structTypeFieldForCommand(component, component, _)),
      Some(prefixedTypeName(component) + structNamePostfix))
  }

  def cStructFieldName(named: HasName, structComponent: Component, component: Component): String =
    (if (structComponent == component) "" else cName(component)) +
      upperCamel2LowerCamel(mangledCName(named).capitalize)

  def mangledCName(named: HasName): String = {
    var methodName = cName(named)
    if (keywords.contains(methodName))
      methodName = "_" + methodName
    methodName
  }

  def cName(named: HasName): String = named.name.mangledNameString()

  def cName(t: DecodeType): String = Option(t.alias) match {
    case Some(a) => cName(a)
    case _ => sys.error("not implemented")
  }

  def structTypeFieldForCommand(component: Component, c: Component, command: Command): CStructTypeDefField = {
    val methodName = cStructFieldName(command, component, component)
    CStructTypeDefField(methodName, CFuncType(cType(command.returnType),
      cFuncParameterTypes(command, component), methodName))
  }

  def cFuncParameterTypes(command: Command, component: Component): Seq[CType] = {
    ptrType(component) +: command.parameters.map(p => {
      val t = p.parameterType
      mapIfNotSmall(cType(t), t, (ct: CType) => ct.ptr)
    })
  }

  def typeWithDependentTypes(t: DecodeType): Set[DecodeType] =
    typeWithDependentTypes(t, Set.empty)

  def typeWithDependentTypes(t: DecodeType, exclude: Set[DecodeType]): Set[DecodeType] =
    (exclude.contains(t) match {
      case true => Set.empty[DecodeType]
      case _ =>
        val extendedExclude = exclude + t
        t match {
          case t: StructType => t.fields.flatMap(f => typeWithDependentTypes(f.typeMeasure.t, extendedExclude)).toSet
          //case t: HasBaseType => t.baseType.typeWithDependentTypes(extendedExclude)
          case t: GenericTypeSpecialized =>
            t.genericTypeArguments.flatMap(a => typeWithDependentTypes(a, extendedExclude)).toSet ++
              typeWithDependentTypes(t.genericType, extendedExclude)
          case _: NativeType => Set.empty[DecodeType]
          case _ => sys.error(s"not implemented for $t")
        }
    }) + t

  def allTypes(component: Component): Set[DecodeType] =
    (component.commands.flatMap(cmd => typeWithDependentTypes(cmd.returnType) ++
      cmd.parameters.flatMap(p => typeWithDependentTypes(p.parameterType))) ++
      component.eventMessages.map(_.baseType) ++
      component.baseType.map(_.fields.flatMap(f => typeWithDependentTypes(f.typeMeasure.t))).getOrElse(Seq.empty)).toSet ++
      allSubComponents(component).flatMap(allTypes)

  def isGeneratable(t: DecodeType): Boolean = t match {
    case n if n.isNative => false
    case _ => true
  }

  def fileName(named: HasName): String = named.name.mangledNameString()

  def cTypeName(named: HasName): String = named.name.mangledNameString()

  def fullMethodDef(eventMessage: EventMessage, component: Component, c: Component): CFuncDef = {
    val eventParam = CFuncParam("event", cType(eventMessage.baseType))
    val eventParams = eventParam +: eventMessage.parameters.flatMap {
      case p: Parameter =>
        val t = p.typeMeasure().t
        Seq(CFuncParam(cName(p.parameterType()), mapIfNotSmall(cType(t), t, (t: CType) => t.constPtr)))
      case _ => Seq.empty
    }
    CFuncDef(fullMethodName(eventMessage, component, c), resultType, eventParams)
  }

  def varName(statusParameter: StatusParameter): String = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL,
    statusParameter.path.toString.replaceAll("[\\.\\[\\]]", "_").replaceAll("__", "_"))


  def serializeMethodName(t: DecodeType): String = methodName(t, typeSerializeMethodName)

  def deserializeMethodName(t: DecodeType): String = methodName(t, typeDeserializeMethodName)

  private def trySerializeFuncCall(t: DecodeType, src: CExpression): CFuncCall = cTry(serializeFuncCall(t, src))

  private def tryDeserializeFuncCall(t: DecodeType, dest: CExpression): CFuncCall = cTry(deserializeFuncCall(t, dest))

  private def deserializeFuncCall(t: DecodeType, dest: CExpression): CFuncCall = cCall(methodName(t, typeDeserializeMethodName), dest, reader.v)

  private def callCodeForBer(t: DecodeType, methodNamePart: String, exprs: CExpression*): CExpression =
    cCall(methodName(photonBerTypeName, methodNamePart), exprs: _*)

  def serializeBerCallCode(t: DecodeType, src: CExpression): CExpression = callCodeForBer(t, typeSerializeMethodName, src, writer.v)

  def deserializeBerCallCode(t: DecodeType, dest: CExpression): CExpression = callCodeForBer(t, typeDeserializeMethodName, dest, reader.v)

  def deserializeCallCode(t: DecodeType, dest: CExpression): CExpression = t match {
    case t: GenericTypeSpecialized if t.isArray => deserializeFuncCall(t, dest)
    case _: StructType => deserializeFuncCall(t, dest)
    case t: NativeType if t.isPrimitive =>
      CAssign(CDeref(dest), callCodeForPrimitiveType(t.primitiveTypeInfo, dest, photonReaderTypeName, "Read", reader.v))
    case n: NativeType => n.isVaruintType match {
      case true => callCodeForBer(t, typeDeserializeMethodName, dest, reader.v)
      case _ => sys.error(s"not implemented for $n")
    }
    /*case t: HasBaseType =>
      val baseType = t.baseType
      baseType.deserializeCallCode(dest.cast(baseType.cType.ptr))*/
    case _ => sys.error(s"not implemented for $t")
  }

  def serializeCallCode(t: DecodeType, src: CExpression): CExpression = t match {
    case t: GenericTypeSpecialized if t.isArray => serializeFuncCall(t, src)
    case _: StructType => serializeFuncCall(t, src)
    case t: NativeType if t.isPrimitive =>
      callCodeForPrimitiveType(t.primitiveTypeInfo, src, photonWriterTypeName, "Write", writer.v, src)
    case t: NativeType => t.isVaruintType match {
      case true => callCodeForBer(t, typeSerializeMethodName, src, writer.v)
      case _ => sys.error(s"not implemented for $t")
    }
    /*case t: HasBaseType =>
      t.baseType.serializeCallCode(src)*/
    case _ =>
      serializeFuncCall(t, src)
  }

  def callCodeForPrimitiveType(t: PrimitiveTypeInfo, src: CExpression, typeName: String, methodPrefix: String,
                               exprs: CExpression*): CFuncCall = {
    import TypeKind._
    cCall(methodName(typeName, methodPrefix + ((t.kind, t.bitLength) match {
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
    })), exprs: _*)
  }

  private def serializeFuncCall(t: DecodeType, src: CExpression): CFuncCall = cCall(methodName(t, typeSerializeMethodName), src, writer.v)

  private def serializeGenericTypeSpecializedCode(t: GenericTypeSpecialized, src: CExpression): CAstElements = {
    val _isSmall = isSmall(t)
    t.genericType match {
      case or if or.isOrType =>
        val tagField = if (_isSmall) cDot(src, tagVar) else ->(src, tagVar)
        CStatementLine(tryCall(methodName(photonBerTypeName, typeSerializeMethodName), tagField, writer.v)) +:
          Seq(CIndent, CSwitch(tagField, t.genericTypeArguments.zipWithIndex.map { case (omp, idx) =>
            val valueVar = CVar("_" + (idx + 1))
            CCase(CIntLiteral(idx),
              Seq(CStatementLine(serializeCallCode(omp,
                mapIfNotSmall(if (_isSmall) cDot(src, valueVar) else ->(src, valueVar), omp,
                  (expr: CExpression) => ref(expr)))), CIndent, CBreak, CSemicolon, CEol))
          }, default = CStatements(CReturn(invalidValue))), CEol)
      case optional if optional.isOptionType =>
        val flagField = if (_isSmall) cDot(src, flagVar) else ->(src, flagVar)
        CStatementLine(tryCall(methodName(photonBerTypeName, typeSerializeMethodName),
          flagField, writer.v)) +:
          Seq(CIndent, CIf(flagField, CEol +:
            serializeCode(t.genericTypeArguments.head,
              if (_isSmall) cDot(src, valueVar) else ->(src, valueVar))))
      case _ => sys.error(s"not implemented $t")
    }
  }

  def ref(expr: CExpression): CExpression = expr match {
    case expr: CDeref => expr.expr
    case _ => CRef(expr)
  }

  private def deserializeGenericTypeSpecializedCode(t: GenericTypeSpecialized, dest: CExpression): CAstElements =
    t.genericType match {
      case or if or.isOrType =>
        CStatementLine(tryCall(methodName(photonBerTypeName, typeDeserializeMethodName), ref(->(dest, tagVar)), reader.v)) +:
          Seq(CIndent, CSwitch(->(dest, tagVar), t.genericTypeArguments.zipWithIndex.map { case (omp, idx) =>
            CCase(CIntLiteral(idx),
              Seq(CStatementLine(deserializeCallCode(omp, ref(->(dest, CVar("_" + (idx + 1)))))),
                CIndent, CBreak, CSemicolon, CEol))
          }, default = CStatements(CReturn(invalidValue))), CEol)
      case optional if optional.isOptionType =>
        CStatementLine(tryCall(methodName(photonBerTypeName, typeDeserializeMethodName),
          cast(ref(->(dest, flagVar)), CTypeApplication(photonBerTypeName).ptr), reader.v)) +:
          Seq(CIndent, CIf(->(dest, flagVar), CEol +:
            deserializeCode(t.genericTypeArguments.head, ref(->(dest, valueVar)))))
      case _ => sys.error(s"not implemented $t")
    }

  private val berSizeOf = cCall("sizeof", berType)

  def abstractMinSizeExpr(t: DecodeType, forceParens: Boolean): Option[CExpression] =
    abstractMinSizeExpr(t, Set.empty, forceParens)

  def abstractMinSizeExpr(t: DecodeType, enclosingTypes: Set[DecodeType],
                          forceParens: Boolean): Option[CExpression] = enclosingTypes.contains(t) match {
    case true => None
    case _ =>
      val extendedEnclosingTypes = enclosingTypes + t
      t match {
        case t: NativeType => Some(cCall("sizeof", cType(t)))
        case t: TypeAlias[_, _] => abstractMinSizeExpr(t.obj, extendedEnclosingTypes, forceParens)
        case t: SubType => abstractMinSizeExpr(t.baseType, extendedEnclosingTypes, forceParens)
        case t: StructType =>
          sumExprs(t.fields.map(f => abstractMinSizeExpr(f.typeMeasure.t, extendedEnclosingTypes, forceParens = false)), forceParens)
        case array: GenericTypeSpecialized if array.isArray =>
          sys.error("not implemented")
        /*val baseType = array.baseType
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
        }*/
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

  def concreteMinSizeExpr(t: DecodeType, src: CExpression, isPtr: Boolean): Option[CExpression] = t match {
    case struct: StructType =>
      sumExprs(
        struct.fields.map(f =>
          concreteMinSizeExpr(f.typeMeasure.t, dotOrArrow(src, CVar(cName(t)), isDot = !isPtr), isPtr = false)),
        forceParens = false)
        .map(commentBeginEnd(_, cName(struct)))
    case array: GenericTypeSpecialized if array.isArray =>
      sys.error("not implemented")
    /*val baseType = array.baseType
    baseType.abstractMinSizeExpr(forceParens = true).map(rExpr =>
      CMul(src.dotOrArrow(size.v, isDot = !isPtr),
        commentBeginEnd(rExpr, baseType.cName)))*/
    case _: SubType | _: TypeAlias[_, _] | _: GenericTypeSpecialized => None // todo: yes you can
    case t: EnumType => concreteMinSizeExpr(t.extendsOrBaseType, src, isPtr)
    case _ => abstractMinSizeExpr(t, forceParens = false)
  }

  private def writerSizeCheckCode(t: DecodeType, src: CExpression) =
    concreteMinSizeExpr(t, src, !isSmall(t)).map { sizeExpr =>
      Seq(CIndent, CIf(CLess(cCall("PhotonWriter_WritableSize", writer.v), sizeExpr),
        Seq(CEol, CStatementLine(CReturn(CVar("PhotonResult_NotEnoughSpace"))))))
  }.getOrElse(Seq.empty)

  private def readerSizeCheckCode(t: DecodeType, dest: CExpression) =
    concreteMinSizeExpr(t, dest, isPtr = true).map { sizeExpr =>
      Seq(CIndent, CIf(CLess(cCall("PhotonReader_ReadableSize", reader.v), sizeExpr),
        Seq(CEol, CStatementLine(CReturn(CVar("PhotonResult_NotEnoughData"))))))
    }.getOrElse(Seq.empty)

  def serializeCode(t: DecodeType): CAstElements = serializeCode(t, selfVar)

  def serializeCode(t: DecodeType, src: CExpression): CAstElements = t match {
    case struct: StructType => writerSizeCheckCode(t, src) ++ struct.fields.flatMap { f =>
      val fType = f.typeMeasure.t
      val fVar = CVar(cName(f))
      Seq(CStatementLine(serializeCallCode(fType, refIfNotSmall(dotOrArrow(src, fVar, isSmall(struct)), fType))))
    }
    case array: GenericTypeSpecialized if array.isArray =>
      sys.error("not implemented")
    //writerSizeCheckCode(src) ++ src.serializeCodeForArraySize(array) ++ array.serializeCodeForArrayElements(src)
    case alias: TypeAlias[_, _] => Seq(CStatementLine(serializeCallCode(alias.obj, src)))
    case s: SubType => Seq(CStatementLine(serializeCallCode(s.baseType, src)))
    case enum: EnumType => Seq(CStatementLine(serializeCallCode(enum.extendsOrBaseType, src)))
    case native: NativeType if native.isPrimitive => Seq(CStatementLine(serializeCallCode(native, src)))
    case native: NativeType => Seq(CStatementLine(serializeCallCode(native, src)))
    case gts: GenericTypeSpecialized => serializeGenericTypeSpecializedCode(gts, src)
    case _ => sys.error(s"not implemented for $t")
  }

  def deserializeCode(t: DecodeType): CAstElements = deserializeCode(t, selfVar)

  def deserializeCode(t: DecodeType, dest: CExpression): CAstElements = t match {
    case t: StructType => /*todo: implement or remove: readerSizeCheckCode(dest) ++*/ t.fields.flatMap(f =>
      Seq(CStatementLine(deserializeCallCode(f.typeMeasure.t, ref(->(dest, CVar(cName(f))))))))
    case array: GenericTypeSpecialized if array.isArray =>
      sys.error("not implemented")
    //dest.deserializeCodeForArraySize(t) ++ readerSizeCheckCode(dest) ++ t.deserializeCodeForArrayElements(dest)
    case t: TypeAlias[_, _] => deserializeCode(t.obj, dest)
    /*case t: HasBaseType =>
      val baseType = t.baseType
      Seq(baseType.deserializeCallCode(dest.cast(baseType.cType.ptr)).line)*/
    case _: NativeType => Seq(CStatementLine(deserializeCallCode(t, dest)))
    case t: GenericTypeSpecialized => deserializeGenericTypeSpecializedCode(t, dest)
    case _ => sys.error(s"not implemented for $t")
  }

  val photonWriterTypeName = "PhotonWriter"
  val photonReaderTypeName = "PhotonReader"
  val typeSerializeMethodName = "Serialize"
  val typeDeserializeMethodName = "Deserialize"
  val tagVar = CVar("tag")
  val flagVar = CVar("flag")
  val valueVar = CVar("value")
  val invalidValue = CVar("PhotonResult_InvalidValue")
  val dataVar = CVar("data")

  def serializeBer(v: CVar): CFuncCall = cCall(methodName(photonBerTypeName, typeSerializeMethodName), v, writer.v)

  def serializeCallCode(mp: StatusParameter, rootComponent: Component, component: Component): CAstElements = {
    val mpr = mp.ref(component)
    val sf = mpr.structField.get // must be a structField here

    var isPtr = !isSmall(sf.typeMeasure.t) // if current expr is pointer
    var expr: CExpression = cCall(parameterMethodName(component, mp, rootComponent))
    var astGen: Option[CAstElements => CAstElements] = None

    var t = sf.typeMeasure.t

    for (next <- mpr.path.elements()) {
      next.isElementName match {
        case true =>
          val elementName = next.elementName().get()
          t = t.asInstanceOf[StructType].fields.find(_.name == elementName)
            .getOrElse(sys.error(s"field $elementName not found")).typeMeasure.t

          val isNotSmall = !isSmall(t)
          expr = mapIf(dotOrArrow(expr, CVar(elementName.mangledNameString()), !isPtr), isNotSmall, e => CParens(ref(e)))
          isPtr = isNotSmall

        case _ =>

          val arrayType = t.asInstanceOf[GenericTypeSpecialized] // fixme
          sys.error("not implemented")
        /*t = arrayType.baseType
        val arraySize = arrayType.size
        val rangeType: ArrayType = ArrayType(t.fqn.last, t.namespace, LocalizedString.empty, MaybeProxy(t),
          range.size(arraySize))

        val fold = astGen.getOrElse(id[CAstElements] _)
        val arrExpr = Some(expr)
        val rangeCType = rangeType.cType
        val astPtr = isPtr
        val isRangeFixed = range.max.isDefined && arraySize.min > range.max.get
        val sizeExpr = range.max.map(max =>
          if (isRangeFixed)
            CULongLiteral(max)
          else
            "PHOTON_MIN".call(CULongLiteral(max), "array"._var.dotOrArrow(size.v, !isPtr)))
          .getOrElse("array"._var.dotOrArrow(size.v, !isPtr))

        astGen = Some((inner: CAstElements) => fold(
          Seq(
            CVarDef("array", if (astPtr) rangeCType.ptr else rangeCType, arrExpr).line,
            CVarDef(size.name, size.t, Some(sizeExpr)).line) ++
          (if (!isRangeFixed) CAstElements(size.v.serializeCallCodeForArraySize.line) else CAstElements.empty) ++
          CAstElements(CIndent,
            CForStatement(Seq(CVarDef(i.name, i.t, Some(CULongLiteral(range.min)))),
              Seq(CLess(i.v, size.v)), Seq(CIncBefore(i.v)), inner).eol)))

        val isNotSmall = !t.isSmall
        expr = "array"._var.dotOrArrow(dataVar, !isPtr)("i"._var).mapIf(isNotSmall, e => CParens(e.ref))
        isPtr = isNotSmall*/

      }
    }
    astGen.getOrElse{ a: CAstElements => a }(Seq(CStatementLine(cTry(serializeCallCode(t, expr)))))
  }

  def serializeCodeForArraySize(expr: CExpression, t: GenericTypeSpecialized): CAstElements = {
    assert(t.isArray) // fixme: static typing
    val sizeExpr = isSmall(t) match {
        case false => ->(expr, size.v)
        case _ => cDot(expr, size.v)
      }
    CStatements(serializeCallCodeForArraySize(sizeExpr))
  }

  def serializeCallCodeForArraySize(expr: CExpression): CFuncCall =
    cTry(cCall(methodName(photonBerTypeName, typeSerializeMethodName), Seq(expr, writer.v): _*))

  def deserializeCodeForArraySize(expr: CExpression, t: GenericTypeSpecialized): CAstElements = {
    assert(t.isArray) // fixme: static typing
    val sizeExpr = ->(expr, size.v)
    CStatements(deserializeCallCodeForArraySize(sizeExpr))
  }

  def deserializeCallCodeForArraySize(expr: CExpression): CFuncCall =
    tryCall(methodName(photonBerTypeName, typeDeserializeMethodName), Seq(ref(expr), reader.v): _*)

  def serializeCodeForArrayElements(src: CExpression): CAstElements = {
    sys.error("not implemented")
    /*val baseType = t.baseType
    val dataExpr = t.dotOrArrow(src, dataVar)(i.v)
    val sizeExpr = t.dotOrArrow(src, size.v)
    Seq(CIndent, CForStatement(Seq(i.v.define(i.t, Some(CIntLiteral(0))), CComma, size.v.assign(sizeExpr)),
      Seq(CLess(i.v, size.v)), Seq(CIncBefore(i.v)),
      Seq(baseType.serializeCallCode(mapIfNotSmall(dataExpr, baseType, (expr: CExpression) => expr.ref)).line)), CEol)*/
  }

  def deserializeCodeForArrayElements(dest: CExpression): CAstElements = {
    sys.error("not implemented")
    /*val baseType = t.baseType
    val dataExpr = dest -> dataVar(i.v)
    Seq(CIndent, CForStatement(Seq(i.v.define(i.t, Some(CIntLiteral(0))), CComma, size.v.assign(dest -> size.v)),
      Seq(CLess(i.v, size.v)), Seq(CIncBefore(i.v)), Seq(baseType.deserializeCallCode(dataExpr.ref).line)), CEol)*/
  }

}
