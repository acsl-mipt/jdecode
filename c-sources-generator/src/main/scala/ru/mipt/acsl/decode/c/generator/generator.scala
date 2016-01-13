package ru.mipt.acsl.decode.c.generator

import java.io
import java.io.{OutputStreamWriter, FileOutputStream}
import java.security.MessageDigest

import com.typesafe.scalalogging.LazyLogging
import ru.mipt.acsl.decode.model.domain._
import ru.mipt.acsl.generation.Generator
import ru.mipt.acsl.generator.c.ast._

import resource._

import scala.collection.mutable
import scala.util.Random

case class CGeneratorConfiguration(outputDir: io.File, registry: DecodeRegistry, rootComponentFqn: String,
                                   namespaceAliases: Map[DecodeFqn, DecodeFqn] = Map.empty)

object CSourcesGenerator {
  private val voidType = CTypeApplication("void")
  private val sizeTType = CTypeApplication("size_t")
  private val stringType = CTypeApplication("char").ptr
  private val commandExecuteResult = CTypeApplication("PhotonCommandExecutionResult")

  private val readerParameter = Parameter("reader", CTypeApplication("PhotonReader").ptr)
  private val writerParameter = Parameter("writer", CTypeApplication("PhotonWriter").ptr)

  private val readerVar = CVar("reader")
  private val writerVar = CVar("writer")

  private val executeCommandMethodName = "executeCommand"
  private val executeCommandParameters = Seq(Parameter("commandId", sizeTType), readerParameter, writerParameter)
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

  private def dirPathForNs(ns: DecodeNamespace): String = nsOrAliasCppSourceParts(ns).mkString("/")

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

  def writeFile(file: io.File, cppFile: CFile.Type) {
    for (typeHeaderStream <- managed(new OutputStreamWriter(new FileOutputStream(file)))) {
      cppFile.generate(CGeneratorState(typeHeaderStream))
    }
  }

  def writeFileIfNotEmptyWithComment[A >: CAstElement](file: io.File, cppFile: CFile.Type, comment: String) {
    if (cppFile.nonEmpty)
      writeFile(file, CFile(Seq(CComment(comment), CEol) ++ cppFile.statements: _*))
  }

  def generateNs(ns: DecodeNamespace) {
    val nsDir = ensureDirForNsExists(ns)
    ns.subNamespaces.foreach(generateNs)
    val typesHeader = CFile()
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

  private def cppTypeNameFromOptionName(name: Option[DecodeName]): String = {
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
      case t: DecodeOptionNamed => cppTypeNameFromOptionName(t.optionName)
      case _ => sys.error("not implemented")
    }
  }

  private val rand = new Random()

  private def protectDoubleIncludeFile(file: CFile.Type): CFile.Type = {
    val bytes = Array[Byte](10)
    rand.nextBytes(bytes)
    val uniqueName: String = "__" ++ MessageDigest.getInstance("MD5").digest(bytes).map("%02x".format(_)).mkString ++ "__"
    CFile(CIfNDef(uniqueName, Seq(CDefine(uniqueName))) +: file.statements :+ CEndIf(): _*)
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
        if (newName equals oldName) CComment("omitted due name clash") else CDefine(newName, oldName)
      case _ => sys.error("not implemented")
    }
    writeFileIfNotEmptyWithComment(tFile, CFile(inner), "Type header")
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

  private def classNameForComponent(comp: DecodeComponent): String = comp.name.asMangledString + "Component"

  private def includePathForComponent(comp: DecodeComponent): String = {
    val dir = dirPathForNs(comp.namespace)
    val className = classNameForComponent(comp)
    val hFileName = className + ".h"
    dir + "/" + hFileName
  }

  private def importStatementsForComponent(comp: DecodeComponent) = {
    val imports = comp.subComponents.flatMap { cr => Seq(CInclude(includePathForComponent(cr.component.obj)), CEol) }
    if (imports.nonEmpty)
      imports += CEol
    val typeIncludes = typesForComponent(comp).flatMap { t => Seq(CInclude(relPathForType(t)), CEol) }
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

  private def methodNameForDecodeName(name: DecodeName): String = {
    var methodName = name.asMangledString
    if (keywords.contains(methodName))
      methodName = "_" + methodName
    methodName
  }

  private def appendPrologEpilog(file: CFile.Type): CFile.Type = {
    CFile(Seq(CInclude("decode_prologue.h"), CEol, CEol) ++ file.statements ++ Seq(CEol, CInclude("decode_epilogue.h"), CEol): _*)
  }

  private def readingAndExecutingCommandsMethods() = {
    Seq(CFuncImpl(CFuncDef("readExecuteCommand", commandExecuteResult, mutable.Buffer(readerParameter, writerParameter)),
      CStatements(
        CIf(FuncCall("decode::canNotReadBer"), CStatements(Return(CVar("decode::CommandExecutionResult::NotEnoughData")))),
        Return(FuncCall("executeCommand", FuncCall("decode::readBerOrFail", readerVar), readerVar, CVar("writer"))))),
      CFuncImpl(CFuncDef(executeCommandMethodName, commandExecuteResult, executeCommandParameters.to[mutable.Buffer]), CStatements()))
  }

  private def generateComponent(comp: DecodeComponent) {
    val dir = dirForNs(comp.namespace)
    val className = classNameForComponent(comp)
    val hFileName = className + ".h"
    val hFile = new io.File(dir, hFileName)
    val cppFile = new io.File(dir, comp.name.asMangledString + "Component.cpp")
    val nsParts = nsOrAliasCppSourceParts(comp.namespace)
    val cppNs = nsParts.mkString("::")
    val imports = importStatementsForComponent(comp)
    val methods = comp.commands.flatMap{cmd =>
      val methodName = methodNameForDecodeName(cmd.name)
      val returnType = cmd.returnType.map { rt => cTypeForDecodeType(rt.obj) }.getOrElse(voidType)
      val parameters = cmd.parameters.map { p => Parameter(p.name.asMangledString, cTypeForDecodeType(p.paramType.obj)) }
      Seq(
        CFuncDef(methodName, returnType, parameters.to[mutable.Buffer]),
        CFuncImpl(CFuncDef(methodName, returnType, mutable.Buffer(readerParameter, writerParameter)),
          if (parameters.isEmpty) {
            val methodCall = FuncCall(methodName)
            CStatements(
              CStatement(MacroCall("BMCL_UNUSED", CVar(readerParameter.name))),
              CStatement(MacroCall("BMCL_UNUSED", CVar(writerParameter.name))),
              if (cmd.returnType.isDefined) Return(methodCall) else CStatement(methodCall))
          } else CStatements())
      )}
    methods ++= comp.messages.map{msg => CFuncDef("write" + msg.name.asMangledString.capitalize, voidType,
      mutable.Buffer(writerParameter))}
    methods ++= comp.baseType.map{bt => bt.obj.fields.map{f =>
      CFuncDef(methodNameForDecodeName(f.name), cTypeForDecodeType(f.typeUnit.t.obj))}}
      .getOrElse(Seq.empty)
    methods ++= readingAndExecutingCommandsMethods()
    //val idField = ClassFieldDef("ID", sizeTType, static = true)
    //val guidField = ClassFieldDef("GUID", stringType, static = true)
    //val fields = Seq(idField, guidField)
    //val classDef = ClassDef(className, methods, comp.subComponents.map { cr => classNameForComponent(cr.component.obj) }, fields)
    val nsClassPrefix = s"$cppNs::$className::"
    //val guidFieldInit = ClassFieldInit(s"$nsClassPrefix${guidField.name}", guidField,
    //  CStringLiteral(comp.fqn.asMangledString))
    //val idFieldInit = ClassFieldInit(s"$nsClassPrefix${idField.name}", idField,
    //  CIntLiteral(componentIdByComponent.get(comp).get))
    val executeCommandImpl = ClassMethodImpl(s"$nsClassPrefix$executeCommandMethodName", commandExecuteResult,
      executeCommandParameters, CStatements(CSwitch(CVar("commandId"), casesForCommands(comp),
        default = Some(CStatements(Return(CVar("PhotonCommandExecutionResult_InvalidCommandId")))))))
    writeFileIfNotEmptyWithComment(hFile, protectDoubleIncludeFile(appendPrologEpilog(CFile(imports ++ Seq(CEol): _*))), s"Component ${comp.name.asMangledString} interface")
    writeFileIfNotEmptyWithComment(cppFile, CFile(CInclude(hFileName), CEol, CEol, executeCommandImpl), s"Component ${comp.name.asMangledString} implementation")
  }

  private def casesForCommands(comp: DecodeComponent): Seq[CCase] = {
    var commandNextId = 0
    val commandsById = mutable.HashMap.empty[Int, (DecodeComponent, DecodeCommand)]
    comp.commands.foreach { cmd =>
      assert(commandsById.put(cmd.id.getOrElse({commandNextId += 1; commandNextId - 1}), (comp, cmd)).isEmpty)
    }
    subComponentsFor(comp).toSeq.sortBy(_.fqn.asMangledString).filterNot(_ == comp).foreach { comp =>
      comp.commands.foreach { cmd =>
        assert(commandsById.put(commandNextId, (comp, cmd)).isEmpty)
        commandNextId += 1
      }
    }
    commandsById.toSeq.sortBy(_._1).map(el => {
      CCase(CIntLiteral(el._1), CStatements(Return(FuncCall((if (el._2._1 == comp) "" else classNameForComponent(el._2._1) + "::") + methodNameForDecodeName(el._2._2.name), readerVar, writerVar))))
    }: CCase)
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
