package ru.mipt.acsl.decode.cpp.generator

import java.io
import java.io.{OutputStreamWriter, FileOutputStream}
import java.security.MessageDigest

import com.typesafe.scalalogging.LazyLogging
import ru.mipt.acsl.decode.model.domain.TypeKind
import ru.mipt.acsl.decode.model.domain._
import ru.mipt.acsl.generation.Generator
import ru.mipt.acsl.generator.cpp.ast._

import resource._

import scala.collection.mutable
import scala.collection.immutable
import scala.util.Random

class CppGeneratorConfiguration(val outputDir: io.File, val registry: DecodeRegistry, val rootComponentFqn: String,
                                val namespaceAlias: Map[DecodeFqn, DecodeFqn] = Map(), val usePragmaOnce: Boolean = true)

object CppSourcesGenerator {
  private val voidType = CppTypeApplication("void")
  private val stdSizeTType = CppTypeApplication("std::size_t")
  private val stdStringType = CppTypeApplication("std::string")
  private val commandExecuteResult = CppTypeApplication("decode::CommandExecutionResult")

  private val readerParameter = Parameter("reader", RefType(CppTypeApplication("bmcl::Reader")))
  private val writerParameter = Parameter("writer", RefType(CppTypeApplication("bmcl::Writer")))

  private val readerVar = CppVar("reader")
  private val writerVar = CppVar("writer")

  private val executeCommandMethodName = "executeCommand"
  private val executeCommandParameters = Seq(Parameter("commandId", stdSizeTType), readerParameter, writerParameter)


}

class CppSourcesGenerator(val config: CppGeneratorConfiguration) extends Generator[CppGeneratorConfiguration] with LazyLogging {

  import CppSourcesGenerator._

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
    config.namespaceAlias.getOrElse(ns.fqn, ns.fqn).parts.map(_.asMangledString)
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

  def writeFile(file: io.File, cppFile: CppFile.Type) {
    for (typeHeaderStream <- managed(new OutputStreamWriter(new FileOutputStream(file)))) {
      cppFile.generate(CppGeneratorState(typeHeaderStream))
    }
  }

  def writeFileIfNotEmptyWithComment[A >: CppAstElement](file: io.File, cppFile: CppFile.Type, comment: String) {
    if (cppFile.nonEmpty)
      writeFile(file, CppFile(Seq(Comment(comment), Eol) ++ cppFile.statements: _*))
  }

  def generateNs(ns: DecodeNamespace) {
    val nsDir = ensureDirForNsExists(ns)
    ns.subNamespaces.foreach(generateNs)
    val typesHeader = File()
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
    case t: Named => t.name.asMangledString
    case t: DecodeArrayType =>
      val baseTypeFileName: String = fileNameFor(t.baseType.obj)
      val min = t.size.min
      val max = t.size.max
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
    case t: OptionNamed => fileNameFromOptionName(t.optionName)
    case _ => sys.error("not implemented")
  }

  private def cppTypeNameFor(t: DecodeType): String = {
    t match {
      case t: Named => t.name.asMangledString
      case t: DecodePrimitiveType => primitiveTypeToCTypeApplication(t).name
      case t: DecodeArrayType =>
        val baseCType: String = cppTypeNameFor(t.baseType.obj)
        val min = t.size.min
        val max = t.size.max
        "DECODE_ARRAY_TYPE_" + ((t.isFixedSize, min, max) match {
          case (true, 0, _) | (false, 0, 0) => s"NAME($baseCType)"
          case (true, _, _) => s"FIXED_SIZE_NAME($baseCType, $min)"
          case (false, 0, _) => s"MAX_NAME($baseCType, $max)"
          case (false, _, 0) => s"MIN_NAME($baseCType, $min)"
          case (false, _, _) => s"MIN_MAX_NAME($baseCType, $min, $max)"
        })
      case t: DecodeGenericTypeSpecialized =>
        cppTypeNameFor(t.genericType.obj) + "_" +
        t.genericTypeArguments.map(tp => if (tp.isDefined) cppTypeNameFor(tp.get.obj) else "void").mkString("_")
      case t: OptionNamed => cppTypeNameFromOptionName(t.optionName)
      case _ => sys.error("not implemented")
    }
  }

  private val rand = new Random()

  private def protectDoubleIncludeFile(file: File.Type): File.Type = {
    if (config.usePragmaOnce) {
      File(Seq(CppPragma("once"), Eol) ++ file.statements: _*)
    } else {
      val bytes = Array[Byte](10)
      rand.nextBytes(bytes)
      val uniqueName: String = "__" ++ MessageDigest.getInstance("MD5").digest(bytes).map("%02x".format(_)).mkString ++ "__"
      File(CppIfNDef(uniqueName, Seq(CppDefine(uniqueName))) +: file.statements :+ CppEndIf(): _*)
    }
  }

  private def primitiveTypeToCTypeApplication(primitiveType: DecodePrimitiveType): CppTypeApplication = {
    import TypeKind._
    (primitiveType.kind, primitiveType.bitLength) match {
      case (Bool, 8) => CppTypeApplication("b8")
      case (Bool, 16) => CppTypeApplication("b16")
      case (Bool, 32) => CppTypeApplication("b32")
      case (Bool, 64) => CppTypeApplication("b64")
      case (Float, 32) => CppFloatType
      case (Float, 64) => CppDoubleType
      case (Int, 8) => CppUnsignedCharType
      case (Int, 16) => CppUnsignedShortType
      case (Int, 32) => CppUnsignedIntType
      case (Int, 64) => CppUnsignedLongType
      case (Uint, 8) => CppSignedCharType
      case (Uint, 16) => CppSignedShortType
      case (Uint, 32) => CppSignedIntType
      case (Uint, 64) => CppSignedLongType
      case _ => sys.error("illegal bit length")
    }
  }

  def cTypeDefForName(t: DecodeType, cType: CppType) = CppTypeDefStatement(cppTypeNameFor(t), cType)
  def cTypeAppForTypeName(t: DecodeType): CppTypeApplication = CppTypeApplication(cppTypeNameFor(t))

  private def cppNsOuterInnerFor(ns: DecodeNamespace) = {
    var outerNs: Option[MutableNamespace.Type] = None
    var innerNs: Option[MutableNamespace.Type] = None
    nsOrAliasCppSourceParts(ns).foreach { part =>
      if (outerNs.isDefined) {
        outerNs = Some(MutableNamespace(part, outerNs.get))
      } else {
        outerNs = Some(MutableNamespace(part))
        innerNs = outerNs
      }
    }
    (outerNs.get, innerNs.get)
  }

  private def generateType(t: DecodeType, nsDir: io.File) {
    if (t.isInstanceOf[DecodePrimitiveType])
      return
    val tFile = new io.File(nsDir, fileNameFor(t) + ".h")
    val (outerNs, innerNs) = cppNsOuterInnerFor(t.namespace)
    innerNs += (t match {
      case t: DecodePrimitiveType => cTypeDefForName(t, cTypeAppForTypeName(t))
      case t: DecodeNativeType => cTypeDefForName(t, CppVoidType.ptr())
      case t: DecodeSubType => cTypeDefForName(t, cTypeAppForTypeName(t.baseType.obj))
      case t: DecodeEnumType => cTypeDefForName(t,
        CppEnumTypeDef(t.constants.map(c => CEnumTypeDefConst(c.name.asMangledString, c.value.toInt))))
      case t: DecodeArrayType => cTypeDefForName(t, CppVoidType.ptr())
      case t: DecodeStructType => cTypeDefForName(t, CppStructTypeDef(t.fields.map(f => CStructTypeDefField(f.name.asMangledString, cTypeAppForTypeName(f.typeUnit.t.obj)))))
      case t: DecodeAliasType =>
        val newName: String = cppTypeNameFor(t)
        val oldName: String = cppTypeNameFor(t.baseType.obj)
        if (newName equals oldName) Comment("omitted due name clash") else CppDefine(newName, oldName)
      case _ => sys.error("not implemented")
    })
    writeFileIfNotEmptyWithComment(tFile, File(outerNs), "Type header")
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

  def cppTypeForDecodeType(t: DecodeType): CppType = {
    t.optionName.map{on => CppTypeApplication(on.asMangledString)}.getOrElse(CppTypeApplication("<not implemented>"))
  }

  private def classNameForComponent(comp: DecodeComponent): String = comp.name.asMangledString + "Component"

  private def includePathForComponent(comp: DecodeComponent): String = {
    val dir = dirPathForNs(comp.namespace)
    val className = classNameForComponent(comp)
    val hFileName = className + ".h"
    dir + "/" + hFileName
  }

  private def importStatementsForComponent(comp: DecodeComponent): immutable.Seq[CppAstElement] = {
    val imports: mutable.Buffer[CppAstElement] = comp.subComponents.flatMap { cr =>
      Seq(CppInclude(includePathForComponent(cr.component.obj)), Eol)
    }.to[mutable.Buffer]
    if (imports.nonEmpty)
      imports += Eol
    val typeIncludes = typesForComponent(comp).flatMap { t => Seq(CppInclude(relPathForType(t)), Eol) }
    imports ++= typeIncludes
    if (typeIncludes.nonEmpty)
      imports += Eol
    imports.to[immutable.Seq]
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

  private def appendPrologEpilog(file: File.Type): File.Type = {
    File(Seq(CppInclude("decode_prologue.h"), Eol, Eol) ++ file.statements ++ Seq(Eol, CppInclude("decode_epilogue.h"), Eol): _*)
  }

  private def readingAndExecutingCommandsMethods() = {
    Seq(ClassMethodDef("readExecuteCommand", commandExecuteResult, mutable.Buffer(readerParameter, writerParameter),
      implementation = CppStatements(
        CppIf(FuncCall("decode::canNotReadBer"), CppStatements(Return(CppVar("decode::CommandExecutionResult::NotEnoughData")))),
        Return(MethodCall("executeCommand", MethodCall("decode::readBerOrFail", readerVar), readerVar, CppVar("writer"))))),
      ClassMethodDef(executeCommandMethodName, commandExecuteResult, executeCommandParameters.to[mutable.Buffer]))
  }

  private def generateComponent(comp: DecodeComponent) {
    val dir = dirForNs(comp.namespace)
    val className = classNameForComponent(comp)
    val hFileName = className + ".h"
    val hFile = new io.File(dir, hFileName)
    val cppFile = new io.File(dir, comp.name.asMangledString + "Component.cpp")
    val (outerHNs, innerHNs) = cppNsOuterInnerFor(comp.namespace)
    val nsParts = nsOrAliasCppSourceParts(comp.namespace)
    val cppNs = nsParts.mkString("::")
    val imports = importStatementsForComponent(comp)
    val methods = comp.commands.flatMap{cmd =>
      val methodName = methodNameForDecodeName(cmd.name)
      val returnType = cmd.returnType.map { rt => cppTypeForDecodeType(rt.obj) }.getOrElse(voidType)
      val parameters = cmd.parameters.map { p => Parameter(p.name.asMangledString, cppTypeForDecodeType(p.paramType.obj)) }
      Seq(
        ClassMethodDef(methodName, returnType, parameters.to[mutable.Buffer], virtual = true, _abstract = true),
        ClassMethodDef(methodName, returnType, mutable.Buffer(readerParameter, writerParameter),
          implementation = if (parameters.isEmpty) {
            val methodCall = MethodCall(methodName)
            CppStatements(
              CppStatement(MacroCall("BMCL_UNUSED", CppVar(readerParameter.name))),
              CppStatement(MacroCall("BMCL_UNUSED", CppVar(writerParameter.name))),
              if (cmd.returnType.isDefined) Return(methodCall) else CppStatement(methodCall))
          } else CppStatements()))
    } ++
      comp.messages.map{msg => ClassMethodDef("write" + msg.name.asMangledString.capitalize, voidType,
          mutable.Buffer(writerParameter))} ++
      comp.baseType.map(_.obj.fields.map { f =>
          ClassMethodDef(methodNameForDecodeName(f.name), cppTypeForDecodeType(f.typeUnit.t.obj))
        }).getOrElse(Seq.empty) ++
      readingAndExecutingCommandsMethods()
    val idField = ClassFieldDef("ID", stdSizeTType, static = true)
    val guidField = ClassFieldDef("GUID", stdStringType, static = true)
    val fields = Seq(idField, guidField)
    val classDef = ClassDef(className, methods, comp.subComponents.map { cr => classNameForComponent(cr.component.obj) }, fields)
    val nsClassPrefix = s"$cppNs::$className::"
    val guidFieldInit = ClassFieldInit(s"$nsClassPrefix${guidField.name}", guidField,
      CppStringLiteral(comp.fqn.asMangledString))
    val idFieldInit = ClassFieldInit(s"$nsClassPrefix${idField.name}", idField,
      CppIntLiteral(componentIdByComponent.get(comp).get))
    val executeCommandImpl = ClassMethodImpl(s"$nsClassPrefix$executeCommandMethodName", commandExecuteResult,
      executeCommandParameters, CppStatements(CppSwitch(CppVar("commandId"), casesForCommands(comp),
        default = Some(CppStatements(Return(CppVar("decode::CommandExecutionResult::InvalidCommandId")))))))
    innerHNs ++= Seq(classDef, Eol)
    writeFileIfNotEmptyWithComment(hFile, protectDoubleIncludeFile(appendPrologEpilog(File(imports :+ outerHNs: _*))), s"Component ${comp.name.asMangledString} interface")
    writeFileIfNotEmptyWithComment(cppFile, CppFile(CppInclude(hFileName), Eol, Eol, guidFieldInit, Eol, Eol, idFieldInit, Eol, Eol, executeCommandImpl), s"Component ${comp.name.asMangledString} implementation")
  }

  private def casesForCommands(comp: DecodeComponent): Seq[CppCase] = {
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
      CppCase(CppIntLiteral(el._1), CppStatements(Return(MethodCall((if (el._2._1 == comp) "" else classNameForComponent(el._2._1) + "::") + methodNameForDecodeName(el._2._2.name), readerVar, writerVar))))
    }: CppCase)
  }

  private def subComponentsFor(comp: DecodeComponent, set: mutable.Set[DecodeComponent] = mutable.HashSet.empty): mutable.Set[DecodeComponent] = {
    comp.subComponents.foreach { ref =>
      val c: DecodeComponent = ref.component.obj
      set += c
      subComponentsFor(c, set)
    }
    set
  }

  override def getConfiguration: CppGeneratorConfiguration = config
}
