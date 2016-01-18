package ru.mipt.acsl.decode.c.generator

import java.io
import java.io.{OutputStreamWriter, FileOutputStream}
import java.security.MessageDigest

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

object CSourcesGenerator {
  private val voidType = CTypeApplication("void")
  private val sizeTType = CTypeApplication("size_t")
  private val stringType = CTypeApplication("char").ptr
  private val commandExecuteResult = CTypeApplication("PhotonCommandExecutionResult")

  private val commandExecuteResultOk = CVar(commandExecuteResult.name + "_Ok")

  private val readerParameter = Parameter("reader", CTypeApplication("PhotonReader").ptr)
  private val writerParameter = Parameter("writer", CTypeApplication("PhotonWriter").ptr)

  private val selfVar = CVar("self")
  private val readerVar = CVar("reader")
  private val writerVar = CVar("writer")

  private val executeCommandMethodName = "executeCommand"
  private val executeCommandParameters = Seq(Parameter("commandId", sizeTType), readerParameter, writerParameter)

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
    dirPathForNs(t.namespace) + io.File.separator + fileNameFor(t) + ".h"
  }

  def ensureDirForNsExists(ns: DecodeNamespace): io.File = {
    val dir = dirForNs(ns)
    if (!(dir.exists() || dir.mkdirs()))
      sys.error(s"Can't create directory ${dir.getAbsolutePath}")
    dir
  }

  def writeFile(file: io.File, cppFile: CAstElements) {
    for (typeHeaderStream <- managed(new OutputStreamWriter(new FileOutputStream(file)))) {
      cppFile.generate(CGeneratorState(typeHeaderStream))
    }
  }

  def writeFileIfNotEmptyWithComment(file: io.File, cppFile: CAstElements, comment: String) {
    if (cppFile.nonEmpty)
      writeFile(file, Seq(CComment(comment), CEol) ++ cppFile)
  }

  def generateNs(ns: DecodeNamespace) {
    val nsDir = ensureDirForNsExists(ns)
    ns.subNamespaces.foreach(generateNs)
    val typesHeader = CAstElements()
    ns.types.foreach(generateType(_, nsDir))
    writeFileIfNotEmptyWithComment(new io.File(nsDir, "types.h"), protectDoubleIncludeFile(typesHeader), s"Types of ${ns.fqn.asMangledString} namespace")
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

  private def fileNameFor(t: DecodeType): String = t match {
    case t: DecodeNamed => t.name.asMangledString
    case t: DecodeArrayType =>
      val baseTypeFileName: String = fileNameFor(t.baseType.obj)
      val min = t.size.minLength
      val max = t.size.maxLength
      baseTypeFileName + "_arr" + ((t.isFixedSize, min, max) match {
        case (true, 0, _) | (false, 0, 0) => ""
        case (true, _, _) => s"_fixed_$min"
        case (false, 0, _) => s"_max_$max"
        case (false, _, 0) => s"_min_$min"
        case (false, _, _) => s"_min_max_${min}_$max"
      })
    case t: DecodeGenericTypeSpecialized =>
      fileNameFor(t.genericType.obj) + "_" +
        t.genericTypeArguments.map(tp => if (tp.isDefined) fileNameFor(tp.get.obj) else "void").mkString("_")
    case t: DecodeOptionNamed => fileNameFromOptionName(t.optionName)
    case _ => sys.error("not implemented")
  }

  private def cTypeNameFor(t: DecodeType): String = {
    t match {
      case t: DecodeNamed => t.name.asMangledString
      case t: DecodePrimitiveType => primitiveTypeToCTypeApplication(t).name
      case t: DecodeArrayType =>
        val baseCType: String = cTypeNameFor(t.baseType.obj)
        val min = t.size.minLength
        val max = t.size.maxLength
        "DECODE_ARRAY_TYPE_" + ((t.isFixedSize, min, max) match {
          case (true, 0, _) | (false, 0, 0) => s"NAME($baseCType)"
          case (true, _, _) => s"FIXED_SIZE_NAME($baseCType, $min)"
          case (false, 0, _) => s"MAX_NAME($baseCType, $max)"
          case (false, _, 0) => s"MIN_NAME($baseCType, $min)"
          case (false, _, _) => s"MIN_MAX_NAME($baseCType, $min, $max)"
        })
      case t: DecodeGenericTypeSpecialized =>
        cTypeNameFor(t.genericType.obj) + "_" +
          t.genericTypeArguments.map(tp => if (tp.isDefined) cTypeNameFor(tp.get.obj) else "void").mkString("_")
      case t: DecodeOptionNamed => cTypeNameFromOptionName(t.optionName)
      case _ => sys.error("not implemented")
    }
  }

  private val rand = new Random()

  private def protectDoubleIncludeFile(file: CAstElements): CAstElements = {
    val bytes = Array[Byte](10)
    rand.nextBytes(bytes)
    val uniqueName: String = "__" ++ MessageDigest.getInstance("MD5").digest(bytes).map("%02x".format(_)).mkString ++ "__"
    Seq(CIfNDef(uniqueName), CEol, CDefine(uniqueName)) ++ file :+ CEndIf
  }

  private def primitiveTypeToCTypeApplication(primitiveType: DecodePrimitiveType): CTypeApplication = {
    import TypeKind._
    (primitiveType.kind, primitiveType.bitLength) match {
      case (Bool, 8) => CTypeApplication("b8")
      case (Bool, 16) => CTypeApplication("b16")
      case (Bool, 32) => CTypeApplication("b32")
      case (Bool, 64) => CTypeApplication("b64")
      case (Float, 32) => CFloatType
      case (Float, 64) => CDoubleType
      case (Int, 8) => CUnsignedCharType
      case (Int, 16) => CUnsignedShortType
      case (Int, 32) => CUnsignedIntType
      case (Int, 64) => CUnsignedLongType
      case (Uint, 8) => CSignedCharType
      case (Uint, 16) => CSignedShortType
      case (Uint, 32) => CSignedIntType
      case (Uint, 64) => CSignedLongType
      case _ => sys.error("illegal bit length")
    }
  }

  def cTypeDefForName(t: DecodeType, cType: CType) = CTypeDefStatement(cTypeNameFor(t), cType)
  def cTypeAppForTypeName(t: DecodeType): CTypeApplication = CTypeApplication(cTypeNameFor(t))

  private def generateType(t: DecodeType, nsDir: io.File) {
    if (t.isInstanceOf[DecodePrimitiveType])
      return
    val tFile = new io.File(nsDir, fileNameFor(t) + ".h")
    val inner = t match {
      case t: DecodePrimitiveType => cTypeDefForName(t, cTypeAppForTypeName(t))
      case t: DecodeNativeType => cTypeDefForName(t, CVoidType.ptr)
      case t: DecodeSubType => cTypeDefForName(t, cTypeAppForTypeName(t.baseType.obj))
      case t: DecodeEnumType => cTypeDefForName(t,
        CEnumTypeDef(t.constants.map(c => CEnumTypeDefConst(c.name.asMangledString, c.value.toInt))))
      case t: DecodeArrayType => cTypeDefForName(t, CVoidType.ptr)
      case t: DecodeStructType => cTypeDefForName(t, CStructTypeDef(t.fields.map(f => CStructTypeDefField(f.name.asMangledString, cTypeAppForTypeName(f.typeUnit.t.obj)))))
      case t: DecodeAliasType =>
        val newName: String = cTypeNameFor(t)
        val oldName: String = cTypeNameFor(t.baseType.obj)
        if (newName equals oldName) CComment("omitted due name clash") else cTypeDefForName(t, cTypeAppForTypeName(t.baseType.obj)) // CDefine(newName, oldName)
      case _ => sys.error("not implemented")
    }
    val imports = mutable.Buffer[CAstElement]((t match {
      case t: DecodeStructType => t.fields.flatMap { f => Seq(CInclude(relPathForType(f.typeUnit.t.obj)), CEol) }
      case t: BaseTyped => if (t.baseType.obj.isInstanceOf[DecodePrimitiveType]) Seq.empty else Seq(CInclude(relPathForType(t.baseType.obj)), CEol)
      case _ => Seq.empty
    }): _*)
    if (imports.nonEmpty)
      imports += CEol
    val externed = externCpp(inner)
    writeFileIfNotEmptyWithComment(tFile, protectDoubleIncludeFile(appendPrologEpilog(imports ++ externed)), "Type header")
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

  def cTypeForDecodeType(t: DecodeType): CType = {
    t.optionName.map{on => CTypeApplication(on.asMangledString)}.getOrElse(CTypeApplication("<not implemented>"))
  }

  private def typeNameForComponent(comp: DecodeComponent): String = comp.name.asMangledString + "Component"

  private def includePathForComponent(comp: DecodeComponent): String = {
    val dir = dirPathForNs(comp.namespace)
    val className = typeNameForComponent(comp)
    val hFileName = className + ".h"
    dir + io.File.separator + hFileName
  }

  private def importStatementsForComponent(comp: DecodeComponent): CAstElements = {
    val imports = CAstElements(comp.subComponents.flatMap { cr => Seq(CInclude(includePathForComponent(cr.component.obj)), CEol) })
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
    Seq(CInclude(prefix + "photon_prologue.h"), CEol, CEol) ++ file ++
      Seq(CEol, CInclude(prefix + "photon_epilogue.h"), CEol, CEol)
  }

  private def readingAndExecutingCommandsMethods() = {
    Seq(CFuncImpl(CFuncDef("readExecuteCommand", commandExecuteResult, Seq(readerParameter, writerParameter)),
        Seq(CIf(CFuncCall("decode::canNotReadBer"), Return(CVar("decode::CommandExecutionResult::NotEnoughData"))),
        Return(CFuncCall("executeCommand", CFuncCall("decode::readBerOrFail", readerVar), readerVar, CVar("writer"))))),
      CFuncImpl(CFuncDef(executeCommandMethodName, commandExecuteResult, executeCommandParameters)))
  }

  def externCpp(file: CAstElements): CAstElements = {
    Seq(CIfDef("__cplusplus"), CEol, CPlainText("extern \"C\" {"), CEol, CEndIf, CEol) ++ file ++
      Seq(CEol, CIfDef("__cplusplus"), CEol, CPlainText("}"), CEol, CEndIf)
  }

  def functionTableTypeNameFor(comp: DecodeComponent): String = typeNameForComponent(comp) + "UserFunctionTable"

  private def ptrTypeFor(comp: DecodeComponent) = {
    CTypeApplication(typeNameForComponent(comp)).ptr
  }

  private def userFuncNameFor(component: DecodeComponent, command: DecodeCommand): String = {
    userFuncComponentStructFieldFor(component) + cNameForDecodeName(command.name).capitalize
  }

  private def generateComponent(comp: DecodeComponent) {
    val dir = dirForNs(comp.namespace)
    val componentStructName = typeNameForComponent(comp)
    val hFileName = componentStructName + ".h"
    val hFile = new io.File(dir, hFileName)
    val cFile = new io.File(dir, comp.name.asMangledString + "Component.c")
    //val nsParts = nsOrAliasCppSourceParts(comp.namespace)
    //val cNs = nsParts.mkString("::")
    val imports = importStatementsForComponent(comp)
    val componentFunctionTableName: String = functionTableTypeNameFor(comp)
    val componentFunctionTableNameStruct: String = componentFunctionTableName + "_s"
    val forwardFuncTableDecl = CForwardStructDecl(componentFunctionTableNameStruct)
    val componentSelfType: CType = ptrTypeFor(comp)
    val methods: Seq[CStructTypeDefField] = comp.commands.flatMap{cmd =>
      val methodName = mangledCNameForDecodeName(cmd.name)
      val returnType = cmd.returnType.map { rt => cTypeForDecodeType(rt.obj) }.getOrElse(voidType)
      val parameters = componentSelfType +: cmd.parameters.map { p => cTypeForDecodeType(p.paramType.obj) }
      Seq(CStructTypeDefField(methodName, CFuncType(returnType, parameters, methodName)))
    }

    val componentType = CTypeDefStatement(componentStructName, CStructTypeDef(Seq(CStructTypeDefField("data", voidType.ptr)) ++
      subComponentsFor(comp).toSeq.flatMap { sc => sc.commands.map { cmd =>
        val returnType = cmd.returnType.map { rt => cTypeForDecodeType(rt.obj) }.getOrElse(voidType)
        val parameters = componentSelfType +: cmd.parameters.map { p => cTypeForDecodeType(p.paramType.obj) }
        val methodName: String = userFuncNameFor(sc, cmd)
        CStructTypeDefField(methodName, CFuncType(returnType, parameters, methodName))
      }} ++
      comp.baseType.map(_.obj.fields.map { f =>
        val name: String = mangledCNameForDecodeName(f.name)
        CStructTypeDefField(name, CFuncType(cTypeForDecodeType(f.typeUnit.t.obj), Seq(componentSelfType), name))
      }).getOrElse(Seq.empty) ++ methods))
    //val vtbl = CTypeDefStatement(componentFunctionTableName, CStructTypeDef(
    //  , Some(componentFunctionTableName + "_s")))

    val execCommand = CFuncImpl(CFuncDef(componentStructName + "_ExecuteCommand", commandExecuteResult,
      Seq(Parameter("self", componentSelfType), readerParameter, writerParameter, Parameter("commandId", sizeTType))),
      CSwitch(CVar("commandId"), casesForCommands(comp),
        default = Some(Return(CVar("PhotonCommandExecutionResult_InvalidCommandId")))))
    val readExecCommand = CFuncDef(componentStructName + "_ReadExecuteCommand", commandExecuteResult,
      Seq(Parameter("self", componentSelfType), readerParameter, writerParameter))
    val functionsForComponentCommands = functionsForCommands(comp)
   /* methods ++= comp.messages.flatMap{msg => Seq(CFuncDef(className + "_Write" + msg.name.asMangledString.capitalize, voidType,
      Seq(writerParameter)), CEol) }
    methods ++= comp.baseType.map{bt => bt.obj.fields.map{f =>
      CFuncDef(methodNameForDecodeName(f.name), cTypeForDecodeType(f.typeUnit.t.obj))}}
      .getOrElse(Seq.empty)
    methods ++= readingAndExecutingCommandsMethods()*/
    //val idField = ClassFieldDef("ID", sizeTType, static = true)
    //val guidField = ClassFieldDef("GUID", stringType, static = true)
    //val fields = Seq(idField, guidField)
    //val classDef = ClassDef(className, methods, comp.subComponents.map { cr => classNameForComponent(cr.component.obj) }, fields)
    //val nsClassPrefix = s"$cNs::$className::"
    //val guidFieldInit = ClassFieldInit(s"$nsClassPrefix${guidField.name}", guidField,
    //  CStringLiteral(comp.fqn.asMangledString))
    //val idFieldInit = ClassFieldInit(s"$nsClassPrefix${idField.name}", idField,
    //  CIntLiteral(componentIdByComponent.get(comp).get))
    //val executeCommandImpl = ClassMethodImpl(s"$nsClassPrefix$executeCommandMethodName", commandExecuteResult,
    //  executeCommandParameters, CStatements(CSwitch(CVar("commandId"), casesForCommands(comp),
    //    default = Some(CStatements(Return(CVar("PhotonCommandExecutionResult_InvalidCommandId")))))))
    val externedCFile = externCpp(Seq(forwardFuncTableDecl, CEol, CEol, componentType, CEol) ++
     functionsForComponentCommands.flatMap { f => Seq(f.definition, CEol) } ++
     Seq(CEol, execCommand.definition, CEol, CEol, readExecCommand, CEol))
    writeFileIfNotEmptyWithComment(hFile, protectDoubleIncludeFile(CEol +: appendPrologEpilog(imports ++ externedCFile)), s"Component ${comp.name.asMangledString} interface")
    writeFileIfNotEmptyWithComment(cFile, Seq(CInclude(hFileName), CEol, CEol) ++
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

  private def defineAndInitVar(v: CVar, parameter: DecodeCommandParameter): Seq[CAstElement] = {
    Seq(CIdent, CDefVar(v.name, cTypeAppForTypeName(parameter.paramType.obj)))
  }

  private def functionsForCommands(comp: DecodeComponent): Seq[CFuncImpl] = {
    val componentTypeName: String = typeNameForComponent(comp)
    val compType = ptrTypeFor(comp)
    val parameters = Seq(Parameter("self", compType), readerParameter, writerParameter)
    commandsByIdFor(comp).toSeq.sortBy(_._1).map(el => {
      val component = el._2.component
      val command = el._2.command
      val methodName: String = typeNameForComponent(component) + cNameForDecodeName(command.name).capitalize
      val vars = command.parameters.map { p => CVar(mangledCNameForDecodeName(p.name))}
      val varInits = vars.zip(command.parameters).flatMap { (el: (CVar, DecodeCommandParameter)) => defineAndInitVar(el._1, el._2) :+ CEol }
      val funcCall = CArrow(selfVar, CFuncCall(userFuncNameFor(component, command), selfVar +: vars: _*))
      CFuncImpl(CFuncDef(componentTypeName + "_" + methodName, commandExecuteResult, parameters),
        varInits ++ Seq(CIdent, CStatement(funcCall), CEol, CIdent, Return(commandExecuteResultOk), CEol))
    })
  }

  private def casesForCommands(comp: DecodeComponent): Seq[CCase] = {
    val componentTypeName: String = typeNameForComponent(comp)
    commandsByIdFor(comp).toSeq.sortBy(_._1).map { el =>
      val methodName: String = typeNameForComponent(el._2.component) + cNameForDecodeName(el._2.command.name).capitalize
      val funcCall: CFuncCall = CFuncCall(componentTypeName + "_" + methodName, selfVar, readerVar, writerVar)
      CCase(CIntLiteral(el._1), Return(funcCall))
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
