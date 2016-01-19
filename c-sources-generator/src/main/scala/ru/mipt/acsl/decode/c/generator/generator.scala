package ru.mipt.acsl.decode.c.generator

import java.io
import java.io.{OutputStreamWriter, FileOutputStream}
import java.security.MessageDigest

import com.google.common.base.{CaseFormat, Charsets}
import com.typesafe.scalalogging.LazyLogging
import ru.mipt.acsl.decode.model.domain._
import ru.mipt.acsl.generation.Generator
import ru.mipt.acsl.generator.c.ast.Implicits._
import ru.mipt.acsl.generator.c.ast._

import resource._

import scala.collection.mutable
import scala.util.Random

case class CGeneratorConfiguration(outputDir: io.File, registry: DecodeRegistry, rootComponentFqn: String,
                                   namespaceAliases: Map[DecodeFqn, DecodeFqn] = Map.empty,
                                   prologEpilogPath: Option[String] = None)

private case class TypedVar(name: String, t: CType) {
  val v = CVar(name)
  val param = CFuncParam(name, t)
}

object CSourcesGenerator {
  private val headerExt = ".h"
  private val sourcesExt = ".c"
  private val structNamePostfix = "_s"
  private val cppDefine = "__cplusplus"
  private val typePrefix = "PhotonGc"

  private val tryMacroName = "PHOTON_TRY"
  private val typeSerializeMethodName = "Serialize"
  private val typeDeserializeMethodName = "Deserialize"

  private val voidType = CTypeApplication("void")
  private val sizeTType = CTypeApplication("size_t")
  private val stringType = CTypeApplication("char").ptr
  private val cDecodeArrayType = CTypeApplication("PhotonArr")

  private val cDecodeArrayTypeInitMethodNamePart = "_Init"

  private val cDecodeArrayTypeInitMethodName = cDecodeArrayType.name + cDecodeArrayTypeInitMethodNamePart

  private val resultType = CTypeApplication("PhotonResult")
  private val resultOk = CVar(resultType.name + "_Ok")

  private val selfVar = CVar("self")

  private val reader = TypedVar("reader", CTypeApplication("PhotonReader").ptr)
  private val writer = TypedVar("writer", CTypeApplication("PhotonWriter").ptr)
  private val commandId = TypedVar("commandId", sizeTType)

  private val executeCommandMethodName = "executeCommand"
  private val executeCommandParameters = Seq(CFuncParam(commandId.name, commandId.t), reader.param, writer.param)

  private val sizeOfStatement = "sizeof"
  private val userFunctionTableStructFieldName = "userFunctionTable"
}

class CSourcesGenerator(val config: CGeneratorConfiguration) extends Generator[CGeneratorConfiguration] with LazyLogging {

  import CSourcesGenerator._

  private var nextComponentId = 0
  private val componentByComponentId = mutable.HashMap.empty[Int, DecodeComponent]
  private val componentIdByComponent = mutable.HashMap.empty[DecodeComponent, Int]

  private def enumerateComponentsFrom(component: DecodeComponent): Unit = {
    if (component.id.isDefined) {
      assert(componentIdByComponent.put(component, component.id.get).isEmpty)
      assert(componentByComponentId.put(component.id.get, component).isEmpty)
    }
    component.subComponents.foreach {cr => enumerateComponentsFrom(cr.component.obj)}
    if (component.id.isEmpty) {
      while (componentByComponentId.contains(nextComponentId))
        nextComponentId += 1
      componentByComponentId.put(nextComponentId, component)
      componentIdByComponent.put(component, nextComponentId)
      nextComponentId += 1
    }
  }

  override def generate() {
    val component: DecodeComponent = config.registry.getComponent(config.rootComponentFqn).get
    enumerateComponentsFrom(component)
    generateRootComponent(component)
  }

  private def nsOrAliasCppSourceParts(ns: DecodeNamespace): Seq[String] = {
    config.namespaceAliases.getOrElse(ns.fqn, ns.fqn).parts.map(_.asMangledString)
  }

  private def dirPathForNs(ns: DecodeNamespace): String = nsOrAliasCppSourceParts(ns).mkString(io.File.separator)

  private def dirForNs(ns: DecodeNamespace): io.File = new io.File(config.outputDir, dirPathForNs(ns))

  private def relPathForType(t: DecodeType) = {
    dirPathForNs(t.namespace) + io.File.separator + fileNameFor(t) + headerExt
  }

  def ensureDirForNsExists(ns: DecodeNamespace): io.File = {
    val dir = dirForNs(ns)
    if (!(dir.exists() || dir.mkdirs()))
      sys.error(s"Can't create directory ${dir.getAbsolutePath}")
    dir
  }

  def writeFile(file: io.File, cppFile: CAstElements) {
    for (typeHeaderStream <- managed(new OutputStreamWriter(new FileOutputStream(file)))) {
      cppFile.generate(CGenState(typeHeaderStream))
    }
  }

  def writeFileIfNotEmptyWithComment(file: io.File, cppFile: CAstElements, comment: String) {
    if (cppFile.nonEmpty)
      writeFile(file, CAstElements(CComment(comment), CEol) ++ cppFile)
  }

  def generateNs(ns: DecodeNamespace) {
    val nsDir = ensureDirForNsExists(ns)
    ns.subNamespaces.foreach(generateNs)
    val typesHeader = CAstElements()
    ns.types.foreach(generateTypeSeparateFiles(_, nsDir))
    val fileName: String = "types" + headerExt
    writeFileIfNotEmptyWithComment(new io.File(nsDir, fileName), protectDoubleIncludeFile(fileName, typesHeader),
      s"Types of ${ns.fqn.asMangledString} namespace")
    //ns.getComponents.toTraversable.foreach(generateRootComponent)
  }

  var fileNameId: Int = 0
  var typeNameId: Int = 0

  private def fileNameFromOptionName(name: Option[DecodeName]): String = {
    if (name.isDefined) {
      name.get.asMangledString
    } else {
      fileNameId += 1
      "type" + fileNameId
    }
  }

  private def cTypeNameFromOptionName(name: Option[DecodeName]): String = {
    if (name.isDefined) {
      name.get.asMangledString
    } else {
      typeNameId += 1
      "type" + typeNameId
    }
  }

  private def fileNameFor(t: DecodeType): String = prefixedCTypeNameFor(t)

  private def lowerUnderscoreToUpperCamel(str: String) = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, str)

  private def prefixedCTypeNameFor(t: DecodeType): String = {
    val tName = cTypeNameFor(t)
    t match {
      case t: DecodePrimitiveType => tName
      case _ => "PhotonGt" + tName
    }
  }

  private def cTypeNameFor(t: DecodeType): String = {
    t match {
      case t: DecodeNamed => lowerUnderscoreToUpperCamel(t.name.asMangledString)
      case t: DecodePrimitiveType => primitiveTypeToCTypeApplication(t).name
      case t: DecodeArrayType =>
        val baseCType: String = cTypeNameFor(t.baseType.obj)
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
        cTypeNameFor(t.genericType.obj) +
          t.genericTypeArguments.map(tp => if (tp.isDefined) cTypeNameFor(tp.get.obj) else "Void").mkString
      case t: DecodeOptionNamed => lowerUnderscoreToUpperCamel(cTypeNameFromOptionName(t.optionName))
      case _ => sys.error("not implemented")
    }
  }

  private val rand = new Random()

  private def protectDoubleIncludeFile(fileName: String, file: CAstElements): CAstElements = {
    val bytes = new Array[Byte](20)
    rand.nextBytes(bytes)
    val uniqueName: String = "__" + fileName.replaceAll("\\.", "_") + "_" + MessageDigest.getInstance("MD5").digest(bytes).map("%02x".format(_)).mkString + "__"
    CAstElements(CIfNDef(uniqueName), CEol, CDefine(uniqueName)) ++ file :+ CEndIf
  }

  private def primitiveTypeToCTypeApplication(primitiveType: DecodePrimitiveType): CTypeApplication = {
    import TypeKind._
    (primitiveType.kind, primitiveType.bitLength) match {
      case (Bool, 8) => CTypeApplication("B8")
      case (Bool, 16) => CTypeApplication("B16")
      case (Bool, 32) => CTypeApplication("B32")
      case (Bool, 64) => CTypeApplication("B64")
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

  def cTypeDefForName(t: DecodeType, cType: CType) = CTypeDefStatement(prefixedCTypeNameFor(t), cType)
  def cTypeAppForTypeName(t: DecodeType): CTypeApplication = CTypeApplication(prefixedCTypeNameFor(t))

  private def generateType(t: DecodeType, nsDir: io.File): (CAstElements, CAstElements) = {
    val (h, c): (CAstElements, CAstElements) = t match {
      case t: DecodePrimitiveType => (CAstElements(cTypeDefForName(t, cTypeAppForTypeName(t))), CAstElements())
      case t: DecodeNativeType => (CAstElements(cTypeDefForName(t, CVoidType.ptr)), CAstElements())
      case t: DecodeSubType => (CAstElements(cTypeDefForName(t, cTypeAppForTypeName(t.baseType.obj))), CAstElements())
      case t: DecodeEnumType => (CAstElements(cTypeDefForName(t,
        CEnumTypeDef(t.constants.map(c => CEnumTypeDefConst(c.name.asMangledString, c.value.toInt))))), CAstElements())
      case t: DecodeArrayType =>
        val sizeVar: CVar = CVar("size")
        val dataVar: CVar = CVar("data")
        val typeDef = CTypeDefStatement(prefixedCTypeNameFor(t), CStructTypeDef(Seq(
          CStructTypeDefField(sizeVar.name, sizeTType),
          CStructTypeDefField(dataVar.name, cTypeForDecodeType(t.baseType.obj).ptr))))
        val initFunc = CFuncImpl(CFuncDef(typeDef.name + cDecodeArrayTypeInitMethodNamePart,
          parameters = Seq(CFuncParam(selfVar.name, typeDef.ptr), CFuncParam(sizeVar.name, sizeTType),
            CFuncParam(dataVar.name, voidType.ptr))), CAstElements(CStatementLine(
          CAssign(CArrow(selfVar, sizeVar), sizeVar)), CStatementLine(CAssign(CArrow(selfVar, dataVar), dataVar))))
        (CAstElements(typeDef, CEol, initFunc.definition),
          CAstElements(initFunc))
      case t: DecodeStructType => (CAstElements(cTypeDefForName(t, CStructTypeDef(t.fields.map(f =>
        CStructTypeDefField(f.name.asMangledString, cTypeAppForTypeName(f.typeUnit.t.obj)))))), CAstElements())
      case t: DecodeAliasType =>
        val newName: String = prefixedCTypeNameFor(t)
        val oldName: String = prefixedCTypeNameFor(t.baseType.obj)
        if (newName equals oldName)
          (CAstElements(CComment("omitted due name clash")), CAstElements())
        else
          (CAstElements(cTypeDefForName(t, cTypeAppForTypeName(t.baseType.obj))), CAstElements()) // CDefine(newName, oldName)
      case _ => sys.error("not implemented")
    }

    val imports: CAstElements = t match {
      case t: DecodeStructType => t.fields.flatMap { f => CAstElements(CInclude(relPathForType(f.typeUnit.t.obj)), CEol) }.to[mutable.Buffer]
      case t: BaseTyped =>
        if (t.baseType.obj.isInstanceOf[DecodePrimitiveType])
          CAstElements()
        else
          CAstElements(CInclude(relPathForType(t.baseType.obj)), CEol)
      case _ => CAstElements()
    }

    val selfType = cTypeAppForTypeName(t).ptr
    val serializeMethod = CFuncImpl(CFuncDef(methodNameFor(t, typeSerializeMethodName), resultType,
      Seq(CFuncParam(selfVar.name, selfType), reader.param)), CAstElements(CReturn(resultOk)))
    val deserializeMethod = CFuncImpl(CFuncDef(methodNameFor(t, typeDeserializeMethodName), resultType,
      Seq(CFuncParam(selfVar.name, selfType), writer.param)), CAstElements(CReturn(resultOk)))

    h ++= Seq(CEol, serializeMethod.definition, CEol, CEol, deserializeMethod.definition)
    c ++= Seq(CEol, serializeMethod, CEol, CEol, deserializeMethod)

    if (imports.nonEmpty)
      imports += CEol
    (imports ++ externCpp(h) ++ Seq(CEol), c)
  }

  private def generateTypeSeparateFiles(t: DecodeType, nsDir: io.File) {
    if (t.isInstanceOf[DecodePrimitiveType])
      return
    val fileName = fileNameFor(t)
    val hFileName = fileName + headerExt
    val cFileName = fileName + sourcesExt
    val (hFile, cFile) = (new io.File(nsDir, hFileName), new io.File(nsDir, cFileName))
    val (h, c) = generateType(t, nsDir)
    writeFileIfNotEmptyWithComment(hFile, protectDoubleIncludeFile(hFileName,
      CAstElements(CEol) ++ appendPrologEpilog(h) ++ Seq(CEol)), "Type header")
    if (c.nonEmpty)
      writeFileIfNotEmptyWithComment(cFile, CAstElements(CInclude(relPathForType(t)), CEol, CEol) ++ c, "Type implementation")
  }

  private def collectNsForType[T <: DecodeType](t: DecodeMaybeProxy[T], set: mutable.Set[DecodeNamespace]) {
    require(t.isResolved, s"Proxy not resolved error for ${t.proxy.toString}")
    collectNsForType(t.obj, set)
  }

  private def collectNsForType(t: DecodeType, set: mutable.Set[DecodeNamespace]) {
    set += t.namespace
    t match {
      case t: BaseTyped => collectNsForType(t.baseType, set)
      case t: DecodeStructType => t.fields.foreach(f => collectNsForType(f.typeUnit.t, set))
      case t: DecodeGenericTypeSpecialized => t.genericTypeArguments
        .filter(_.isDefined).foreach(a => collectNsForType(a.get, set))
      case _ =>
    }
  }

  private def collectNsForTypes(comp: DecodeComponent, set: mutable.Set[DecodeNamespace]) {
    if (comp.baseType.isDefined)
      collectNsForType(comp.baseType.get, set)
    comp.commands.foreach(cmd => {
      cmd.parameters.foreach(arg => collectNsForType(arg.paramType, set))
      if (cmd.returnType.isDefined)
        collectNsForType(cmd.returnType.get, set)
    })
  }

  def collectNsForComponent(comp: DecodeComponent, nsSet: mutable.HashSet[DecodeNamespace]) {
    comp.subComponents.foreach(cr => collectNsForComponent(cr.component.obj, nsSet))
    collectNsForTypes(comp, nsSet)
  }

  def collectComponentsForComponent(comp: DecodeComponent, compSet: mutable.HashSet[DecodeComponent]): Unit = {
    compSet += comp
    comp.subComponents.foreach(cr => collectComponentsForComponent(cr.component.obj, compSet))
  }

  private def generateRootComponent(comp: DecodeComponent) {
    logger.debug(s"Generating component ${comp.name.asMangledString}")
    val nsSet = mutable.HashSet.empty[DecodeNamespace]
    collectNsForComponent(comp, nsSet)
    nsSet.foreach(generateNs)
    val compSet = mutable.HashSet.empty[DecodeComponent]
    collectComponentsForComponent(comp, compSet)
    compSet.foreach(generateComponent)
  }

  def cTypeForDecodeType(t: DecodeType): CType = CTypeApplication(prefixedCTypeNameFor(t))

  private def prefixedTypeNameForComponent(comp: DecodeComponent): String = typePrefix + typeNameForComponent(comp)

  private def typeNameForComponent(comp: DecodeComponent): String = comp.name.asMangledString

  private def includePathForComponent(comp: DecodeComponent): String = {
    val dir = dirPathForNs(comp.namespace)
    val className = prefixedTypeNameForComponent(comp)
    val hFileName = className + headerExt
    dir + io.File.separator + hFileName
  }

  private def importStatementsForComponent(comp: DecodeComponent): CAstElements = {
    val imports = comp.subComponents.flatMap { cr => Seq(CInclude(includePathForComponent(cr.component.obj)), CEol) }
    if (imports.nonEmpty)
      imports += CEol
    val types = typesForComponent(comp).toSeq
    val typeIncludes = types.flatMap { t => Seq(CInclude(relPathForType(t)), CEol) }
    imports ++= typeIncludes
    if (typeIncludes.nonEmpty)
      imports += CEol
    imports
  }

  private def typesForComponent(comp: DecodeComponent, typesSet: mutable.Set[DecodeType] = mutable.HashSet.empty) = {
    typesSet ++= comp.commands.flatMap { cmd =>
      cmd.returnType.map { rt =>
        Seq(rt.obj)
      }.getOrElse(Seq.empty) ++ cmd.parameters.map(_.paramType.obj)
    }
    typesSet ++= comp.baseType.map(_.obj.fields.map(_.typeUnit.t.obj)).getOrElse(Seq.empty)
    logger.debug(comp.name.toString)
    logger.debug(typesSet.toString())
    typesSet
  }

  private val keywords = Seq("return")

  private def cNameForDecodeName(name: DecodeName): String = {
    name.asMangledString
  }

  private def mangledCNameForDecodeName(name: DecodeName): String = {
    var methodName = cNameForDecodeName(name)
    if (keywords.contains(methodName))
      methodName = "_" + methodName
    methodName
  }

  private def appendPrologEpilog(file: CAstElements): CAstElements = {
    val prefix = config.prologEpilogPath.map(_ + io.File.separator).getOrElse("")
    CAstElements(CInclude(prefix + "photon_prologue.h"), CEol, CEol) ++ file ++
      Seq(CEol, CInclude(prefix + "photon_epilogue.h"), CEol, CEol)
  }

  private def readingAndExecutingCommandsMethods(): CAstElements = {
    CAstElements(CFuncImpl(CFuncDef("readExecuteCommand", resultType, Seq(reader.param, writer.param)),
      CAstElements(CIf(CFuncCall("decode::canNotReadBer"), CAstElements(CReturn(CVar("decode::CommandExecutionResult::NotEnoughData")))),
        CReturn(CFuncCall("executeCommand", CFuncCall("decode::readBerOrFail", reader.v), reader.v, writer.v)))),
      CFuncImpl(CFuncDef(executeCommandMethodName, resultType, executeCommandParameters)))
  }

  def externCpp(file: CAstElements): CAstElements = {
    CAstElements(CIfDef(cppDefine), CEol, CPlainText("extern \"C\" {"), CEol, CEndIf, CEol, CEol) ++ file ++
      Seq(CEol, CEol, CIfDef(cppDefine), CEol, CPlainText("}"), CEol, CEndIf)
  }

  def functionTableTypeNameFor(comp: DecodeComponent): String = prefixedTypeNameForComponent(comp) + "UserFunctionTable"

  private def ptrTypeFor(comp: DecodeComponent) = {
    CTypeApplication(prefixedTypeNameForComponent(comp)).ptr
  }

  private def userFuncNameFor(selfComponent: DecodeComponent, component: DecodeComponent, command: DecodeCommand): String = {
    (if (selfComponent == component) "" else userFuncComponentStructFieldFor(component)) + cNameForDecodeName(command.name).capitalize
  }

  private def generateComponent(comp: DecodeComponent) {
    val dir = dirForNs(comp.namespace)
    val componentStructName = prefixedTypeNameForComponent(comp)
    val hFileName = componentStructName + headerExt
    val hFile = new io.File(dir, hFileName)
    val cFile = new io.File(dir, componentStructName + sourcesExt)
    val imports = importStatementsForComponent(comp)
    val componentFunctionTableName = functionTableTypeNameFor(comp)
    val componentFunctionTableNameStruct = componentFunctionTableName + structNamePostfix
    val forwardFuncTableDecl = CForwardStructDecl(componentFunctionTableNameStruct)
    val componentTypeStructName = componentStructName + structNamePostfix
    val componentSelfType: CType = ptrTypeFor(comp)
    val methods: Seq[CStructTypeDefField] = comp.commands.flatMap{cmd =>
      val methodName = mangledCNameForDecodeName(cmd.name)
      val returnType = cmd.returnType.map { rt => cTypeForDecodeType(rt.obj) }.getOrElse(voidType)
      val parameters = componentSelfType +: cmd.parameters.map { p => cTypeForDecodeType(p.paramType.obj) }
      Seq(CStructTypeDefField(methodName, CFuncType(returnType, parameters, methodName)))
    }

    val componentTypeForwardDecl = CForwardStructTypeDef(componentStructName, componentTypeStructName)
    val componentType = CStructTypeDef(Seq(CStructTypeDefField("data", voidType.ptr)) ++
      subComponentsFor(comp).toSeq.flatMap { sc => sc.commands.map { cmd =>
        val returnType = cmd.returnType.map { rt => cTypeForDecodeType(rt.obj) }.getOrElse(voidType)
        val parameters = componentSelfType +: cmd.parameters.map { p => cTypeForDecodeType(p.paramType.obj) }
        val methodName = userFuncNameFor(comp, sc, cmd)
        CStructTypeDefField(methodName, CFuncType(returnType, parameters, methodName))
      }} ++
      comp.baseType.map(_.obj.fields.map { f =>
        val name: String = mangledCNameForDecodeName(f.name)
        CStructTypeDefField(name, CFuncType(cTypeForDecodeType(f.typeUnit.t.obj), Seq(componentSelfType), name))
      }).getOrElse(Seq.empty) ++ methods, Some(componentTypeStructName))

    val execCommand = CFuncImpl(CFuncDef(componentStructName + "_ExecuteCommand", resultType,
      Seq(CFuncParam(selfVar.name, componentSelfType), reader.param, writer.param, commandId.param)),
      CAstElements(CSwitch(commandId.v, casesForCommands(comp),
        default = CAstElements(CReturn(CVar("PhotonResult_InvalidCommandId"))))))
    val readExecCommand = CFuncDef(componentStructName + "_ReadExecuteCommand", resultType,
      Seq(CFuncParam("self", componentSelfType), reader.param, writer.param))
    val functionsForComponentCommands = functionsForCommands(comp)
    val externedCFile = externCpp(CAstElements(forwardFuncTableDecl, CEol, CEol, componentTypeForwardDecl, CEol, CEol, componentType, CSemicolon, CEol) ++
     functionsForComponentCommands.flatMap { f => Seq(f.definition, CEol) } ++
     Seq(CEol, execCommand.definition, CEol, CEol, readExecCommand, CEol))
    writeFileIfNotEmptyWithComment(hFile, protectDoubleIncludeFile(hFileName,
      CEol +: appendPrologEpilog(imports ++ externedCFile)), s"Component ${comp.name.asMangledString} interface")
    writeFileIfNotEmptyWithComment(cFile, CAstElements(CInclude(hFileName), CEol, CEol) ++
      functionsForComponentCommands.flatMap { f => Seq(f, CEol, CEol) } :+
      execCommand,
      s"Component ${comp.name.asMangledString} implementation")
  }

  private def userFuncComponentStructFieldFor(sc: DecodeComponent): String = {
    val name: String = typeNameForComponent(sc)
    Character.toLowerCase(name.charAt(0)) + name.substring(1)
  }

  private case class ComponentCommand(component: DecodeComponent, command: DecodeCommand)

  private def commandsByIdFor(comp: DecodeComponent): mutable.Map[Int, ComponentCommand] = {
    var commandNextId = 0
    val commandsById = mutable.HashMap.empty[Int, ComponentCommand]
    comp.commands.foreach { cmd =>
      assert(commandsById.put(cmd.id.getOrElse({commandNextId += 1; commandNextId - 1}), ComponentCommand(comp, cmd)).isEmpty)
    }
    subComponentsFor(comp).toSeq.sortBy(_.fqn.asMangledString).filterNot(_ == comp).foreach { comp =>
      comp.commands.foreach { cmd =>
        assert(commandsById.put(commandNextId, ComponentCommand(comp, cmd)).isEmpty)
        commandNextId += 1
      }
    }
    commandsById
  }

  private def methodNameFor(t: DecodeType, name: String) = prefixedCTypeNameFor(t) + "_" + name

  private def defineAndInitVar(v: CVar, parameter: DecodeCommandParameter): CAstElements = {
    CAstElements(CStatementLine(CDefVar(v.name, cTypeAppForTypeName(parameter.paramType.obj))),
      CStatementLine(CFuncCall(tryMacroName, CFuncCall(methodNameFor(parameter.paramType.obj, typeDeserializeMethodName), CRef(v), reader.v))))
  }

  private def functionsForCommands(comp: DecodeComponent): Seq[CFuncImpl] = {
    val componentTypeName: String = prefixedTypeNameForComponent(comp)
    val compType = ptrTypeFor(comp)
    val parameters = Seq(CFuncParam(selfVar.name, compType), reader.param, writer.param)
    commandsByIdFor(comp).toSeq.sortBy(_._1).map(el => {
      val component = el._2.component
      val command = el._2.command
      val methodName: String = (if (comp == component) "" else typeNameForComponent(component)) + cNameForDecodeName(command.name).capitalize
      val vars = command.parameters.map { p => CVar(mangledCNameForDecodeName(p.name))}
      val varInits = vars.zip(command.parameters).flatMap { (el: (CVar, DecodeCommandParameter)) =>
        defineAndInitVar(el._1, el._2)
      }.to[mutable.Buffer]
      val funcCall = CArrow(selfVar, CFuncCall(userFuncNameFor(comp, component, command), selfVar +: vars: _*))
      CFuncImpl(CFuncDef(componentTypeName + "_" + methodName, resultType, parameters),
        varInits ++ CAstElements(CIdent, CStatement(funcCall), CEol, CIdent, CReturn(resultOk), CEol))
    })
  }

  private def casesForCommands(comp: DecodeComponent): Seq[CCase] = {
    val componentTypeName: String = prefixedTypeNameForComponent(comp)
    commandsByIdFor(comp).toSeq.sortBy(_._1).map { el =>
      val component = el._2.component
      val command = el._2.command
      val methodName: String = typeNameForComponent(component) + cNameForDecodeName(command.name).capitalize
      val funcCall: CFuncCall = CFuncCall(componentTypeName + "_" + methodName, selfVar, reader.v, writer.v)
      CCase(CIntLiteral(el._1), CAstElements(CReturn(funcCall)))
    }
  }

  private def subComponentsFor(comp: DecodeComponent, set: mutable.Set[DecodeComponent] = mutable.HashSet.empty): mutable.Set[DecodeComponent] = {
    comp.subComponents.foreach { ref =>
      val c: DecodeComponent = ref.component.obj
      set += c
      subComponentsFor(c, set)
    }
    set
  }

  override def getConfiguration: CGeneratorConfiguration = config
}
