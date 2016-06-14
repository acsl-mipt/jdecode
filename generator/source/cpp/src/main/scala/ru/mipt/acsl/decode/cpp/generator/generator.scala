package ru.mipt.acsl.decode.cpp.generator

import java.io
import java.io.{FileOutputStream, OutputStreamWriter}
import java.security.MessageDigest

import scala.collection.JavaConversions._
import com.typesafe.scalalogging.LazyLogging
import ru.mipt.acsl.decode.model.component.{Command, Component}
import ru.mipt.acsl.decode.model.registry.Registry
import ru.mipt.acsl.decode.model.naming._
import ru.mipt.acsl.decode.model.naming.{ElementName, Fqn}
import ru.mipt.acsl.decode.model.proxy.{MaybeProxyCompanion, MaybeTypeProxy}
import ru.mipt.acsl.decode.model.types.{Alias, DecodeType, EnumType, GenericTypeSpecialized, NativeType, PrimitiveTypeInfo, StructType, SubType, TypeAlias, TypeKind}
import ru.mipt.acsl.generator.cpp.ast._

import scala.collection.{immutable, mutable}
import scala.util.Random

class CppGeneratorConfiguration(val outputDir: io.File, val registry: Registry, val rootComponentFqn: String,
                                val namespaceAlias: Map[Fqn, Fqn] = Map(), val usePragmaOnce: Boolean = true)

object CppSourceGenerator {
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

class CppSourceGenerator(val config: CppGeneratorConfiguration) extends LazyLogging {

  import CppSourceGenerator._

  private var nextComponentId = 0
  private val componentByComponentId = mutable.HashMap.empty[Int, Component]
  private val componentIdByComponent = mutable.HashMap.empty[Component, Int]

  private def enumerateComponentsFrom(component: Component): Unit = {
    if (component.id.isPresent) {
      val id = component.id.get()
      assert(componentIdByComponent.put(component, id).isEmpty)
      assert(componentByComponentId.put(id, component).isEmpty)
    }
    component.subComponents.foreach {cr => enumerateComponentsFrom(cr.obj.obj)}
    if (!component.id.isPresent) {
      while (componentByComponentId.contains(nextComponentId))
        nextComponentId += 1
      componentByComponentId.put(nextComponentId, component)
      componentIdByComponent.put(component, nextComponentId)
      nextComponentId += 1
    }
  }

  def generate() {
    val component: Component = config.registry.component(Fqn.newInstance(config.rootComponentFqn)).get
    enumerateComponentsFrom(component)
    generateRootComponent(component)
  }

  private def nsOrAliasCppSourceParts(ns: Namespace): Seq[String] = {
    config.namespaceAlias.getOrElse(ns.fqn, ns.fqn).getParts.map(_.mangledNameString())
  }

  private def dirPathForNs(ns: Namespace): String = nsOrAliasCppSourceParts(ns).mkString("/")

  private def dirForNs(ns: Namespace): io.File = new io.File(config.outputDir, dirPathForNs(ns))

  private def relPathForType(t: DecodeType) =
    dirPathForNs(t.namespace) + io.File.separator + fileNameFor(t) + ".h"

  def ensureDirForNsExists(ns: Namespace): io.File = {
    val dir = dirForNs(ns)
    if (!(dir.exists() || dir.mkdirs()))
      sys.error(s"Can't create directory ${dir.getAbsolutePath}")
    dir
  }

  def writeFile(file: io.File, cppFile: CppFile.Type) {
    val typeHeaderStream = new OutputStreamWriter(new FileOutputStream(file))
    try {
      cppFile.generate(CppGeneratorState(typeHeaderStream))
    } finally {
      typeHeaderStream.close()
    }
  }

  def writeFileIfNotEmptyWithComment[A >: CppAstElement](file: io.File, cppFile: CppFile.Type, comment: String) {
    if (cppFile.nonEmpty)
      writeFile(file, CppFile(Seq(Comment(comment), Eol) ++ cppFile.statements: _*))
  }

  def generateNs(ns: Namespace) {
    val nsDir = ensureDirForNsExists(ns)
    ns.subNamespaces.foreach(generateNs)
    val typesHeader = File()
    ns.types.foreach(generateType(_, nsDir))
    writeFileIfNotEmptyWithComment(new io.File(nsDir, "types.h"), protectDoubleIncludeFile(typesHeader), s"Types of ${ns.fqn.mangledNameString()} namespace")
    //ns.getComponents.toTraversable.foreach(generateRootComponent)
  }

  var fileNameId: Int = 0
  var typeNameId: Int = 0

  private def fileNameFromOptionName(name: Option[ElementName]): String = {
    if (name.isDefined) {
      name.get.mangledNameString()
    } else {
      fileNameId += 1
      "type" + fileNameId
    }
  }

  private def cppTypeNameFromOptionName(name: Option[ElementName]): String = {
    if (name.isDefined) {
      name.get.mangledNameString()
    } else {
      typeNameId += 1
      "type" + typeNameId
    }
  }

  private def fileNameFor(t: DecodeType): String = t match {
    case t: DecodeType if t.isArray =>
      sys.error("not implemented")
      /*val baseTypeFileName: String = fileNameFor(t.baseType)
      val min = t.size.min
      val max = t.size.max
      baseTypeFileName + "_arr" + ((t.isFixedSize, min, max) match {
        case (true, 0, _) | (false, 0, 0) => ""
        case (true, _, _) => s"_fixed_$min"
        case (false, 0, _) => s"_max_$max"
        case (false, _, 0) => s"_min_$min"
        case (false, _, _) => s"_min_max_${min}_$max"
      })*/
    case t: GenericTypeSpecialized =>
      fileNameFor(t.genericType) + "_" +
        t.genericTypeArguments.map(tp => fileNameFor(tp)).mkString("_")
    case t: HasName => t.name.mangledNameString()
    case _ => sys.error("not implemented")
  }

  private def cppTypeNameFor(t: DecodeType): String = {
    t match {
      case t: NativeType => primitiveTypeToCTypeApplication(t).name
      case t: DecodeType if t.isArray =>
        sys.error("not implemented")
        /*
        val baseCType: String = cppTypeNameFor(t.baseType)
        val min = t.size.min
        val max = t.size.max
        "DECODE_ARRAY_TYPE_" + ((t.isFixedSize, min, max) match {
          case (true, 0, _) | (false, 0, 0) => s"NAME($baseCType)"
          case (true, _, _) => s"FIXED_SIZE_NAME($baseCType, $min)"
          case (false, 0, _) => s"MAX_NAME($baseCType, $max)"
          case (false, _, 0) => s"MIN_NAME($baseCType, $min)"
          case (false, _, _) => s"MIN_MAX_NAME($baseCType, $min, $max)"
        })*/
      case t: GenericTypeSpecialized =>
        cppTypeNameFor(t.genericType) + "_" +
        t.genericTypeArguments.map(tp => cppTypeNameFor(tp)).mkString("_")
      case t: HasName => t.name.mangledNameString()
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

  private def primitiveTypeToCTypeApplication(t: NativeType): CppTypeApplication = {
    import TypeKind._
    val p = PrimitiveTypeInfo.typeInfoByFqn(Option(t.fqn.orElse(null)).getOrElse(sys.error("wtf, fix this method, please")))
    (p.kind, p.bitLength) match {
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

  private def cppNsOuterInnerFor(ns: Namespace) = {
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
    if (t.isInstanceOf[NativeType])
      return
    val tFile = new io.File(nsDir, fileNameFor(t) + ".h")
    val (outerNs, innerNs) = cppNsOuterInnerFor(t.namespace)
    innerNs += (t match {
      case t: DecodeType if t.isArray => cTypeDefForName(t, CppVoidType.ptr())
      case t: NativeType =>
        val p = PrimitiveTypeInfo.typeInfoByFqn.get(Option(t.fqn.orElse(null)).getOrElse(sys.error("wtf, fix this")))
        p.map(_ => cTypeDefForName(t, cTypeAppForTypeName(t))).getOrElse(cTypeDefForName(t, CppVoidType.ptr()))
      case t: SubType => cTypeDefForName(t, cTypeAppForTypeName(t.baseType))
      case t: EnumType => cTypeDefForName(t,
        CppEnumTypeDef(t.constants.map(c => CEnumTypeDefConst(c.name.mangledNameString(), c.value.toLongOrFail))))
      case t: StructType =>
        cTypeDefForName(t, CppStructTypeDef(t.fields.map(f =>
          CStructTypeDefField(f.name.mangledNameString(), cTypeAppForTypeName(f.typeMeasure.t)))))
      case t: TypeAlias[_, _] =>
        val newName: String = cppTypeNameFor(t)
        val oldName: String = cppTypeNameFor(t.obj)
        if (newName equals oldName) Comment("omitted due name clash") else CppDefine(newName, oldName)
      case _ => sys.error("not implemented")
    })
    writeFileIfNotEmptyWithComment(tFile, File(outerNs), "Type header")
  }

  private def collectNsForType(t: MaybeTypeProxy, set: mutable.Set[Namespace]) {
    require(t.isResolved, s"Proxy not resolved error for ${t.proxy.toString}")
    collectNsForType(t.obj, set)
  }

  private def collectNsForType(t: DecodeType, set: mutable.Set[Namespace]) {
    set += t.namespace
    t match {
      //case t: HasBaseType => collectNsForType(t.baseType, set)
      case t: StructType => t.fields.foreach(f => collectNsForType(f.typeMeasure.t, set))
      case t: GenericTypeSpecialized => t.genericTypeArguments
        .foreach(a => collectNsForType(a, set))
      case _ =>
    }
  }

  private def collectNsForTypes(comp: Component, set: mutable.Set[Namespace]) {
    if (comp.baseType.isPresent)
      collectNsForType(comp.baseType.get, set)
    comp.commands.foreach(cmd => {
      cmd.parameters.foreach(arg => collectNsForType(arg.parameterType, set))
      collectNsForType(cmd.returnType, set)
    })
  }

  def collectNsForComponent(comp: Component, nsSet: mutable.HashSet[Namespace]) {
    comp.subComponents.foreach(cr => collectNsForComponent(cr.obj.obj, nsSet))
    collectNsForTypes(comp, nsSet)
  }

  def collectComponentsForComponent(comp: Component, compSet: mutable.HashSet[Component]): Unit = {
    compSet += comp
    comp.subComponents.foreach(cr => collectComponentsForComponent(cr.obj.obj, compSet))
  }

  private def generateRootComponent(comp: Component) {
    logger.debug(s"Generating component ${comp.name.mangledNameString()}")
    val nsSet = mutable.HashSet.empty[Namespace]
    collectNsForComponent(comp, nsSet)
    nsSet.foreach(generateNs)
    val compSet = mutable.HashSet.empty[Component]
    collectComponentsForComponent(comp, compSet)
    compSet.foreach(generateComponent)
  }

  def cppTypeForDecodeType(t: DecodeType): CppType =
    CppTypeApplication(Option(t.nameOption.orElse(null)).getOrElse(sys.error("not implemented")).mangledNameString)

  private def classNameForComponent(comp: Component): String = comp.name.mangledNameString() + "Component"

  private def includePathForComponent(comp: Component): String = {
    val dir = dirPathForNs(comp.namespace)
    val className = classNameForComponent(comp)
    val hFileName = className + ".h"
    dir + "/" + hFileName
  }

  private def importStatementsForComponent(comp: Component): immutable.Seq[CppAstElement] = {
    val imports: mutable.Buffer[CppAstElement] = comp.subComponents.flatMap { cr =>
      Seq(CppInclude(includePathForComponent(cr.obj.obj)), Eol)
    }.to[mutable.Buffer]
    if (imports.nonEmpty)
      imports += Eol
    val typeIncludes = typesForComponent(comp).flatMap { t => Seq(CppInclude(relPathForType(t)), Eol) }
    imports ++= typeIncludes
    if (typeIncludes.nonEmpty)
      imports += Eol
    imports.to[immutable.Seq]
  }

  private def typesForComponent(comp: Component, typesSet: mutable.Set[DecodeType] = mutable.HashSet.empty) = {
    typesSet ++= comp.commands.flatMap(cmd => cmd.returnType +: cmd.parameters.map(_.parameterType))
    typesSet ++= Option(comp.baseType.orElse(null)).map(_.fields.map(_.typeMeasure.t)).getOrElse(Seq.empty)
    typesSet
  }

  private val keywords = Seq("return")

  private def methodNameForDecodeName(name: ElementName): String = {
    var methodName = name.mangledNameString()
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

  private def generateComponent(comp: Component) {
    val dir = dirForNs(comp.namespace)
    val className = classNameForComponent(comp)
    val hFileName = className + ".h"
    val hFile = new io.File(dir, hFileName)
    val cppFile = new io.File(dir, comp.name.mangledNameString() + "Component.cpp")
    val (outerHNs, innerHNs) = cppNsOuterInnerFor(comp.namespace)
    val nsParts = nsOrAliasCppSourceParts(comp.namespace)
    val cppNs = nsParts.mkString("::")
    val imports = importStatementsForComponent(comp)
    val methods = comp.commands.flatMap{cmd =>
      val methodName = methodNameForDecodeName(cmd.name)
      val returnType = cppTypeForDecodeType(cmd.returnType)
      val parameters = cmd.parameters.map { p => Parameter(p.name.mangledNameString(), cppTypeForDecodeType(p.parameterType)) }
      Seq(
        ClassMethodDef(methodName, returnType, parameters, virtual = true, _abstract = true),
        ClassMethodDef(methodName, returnType, mutable.Buffer(readerParameter, writerParameter),
          implementation = if (parameters.isEmpty) {
            val methodCall = MethodCall(methodName)
            CppStatements(
              CppStatement(MacroCall("BMCL_UNUSED", CppVar(readerParameter.name))),
              CppStatement(MacroCall("BMCL_UNUSED", CppVar(writerParameter.name))),
              if (cmd.returnType.isUnit) CppStatement(methodCall) else Return(methodCall))
          } else CppStatements()))
    } ++
      (comp.statusMessages ++ comp.eventMessages).map{msg =>
        ClassMethodDef("write" + msg.name.mangledNameString().capitalize, voidType, mutable.Buffer(writerParameter))
      } ++
      Option(comp.baseType.orElse(null)).map(_.fields.map { f =>
          ClassMethodDef(methodNameForDecodeName(f.name), cppTypeForDecodeType(f.typeMeasure.t))
        }).getOrElse(Seq.empty) ++
      readingAndExecutingCommandsMethods()
    val idField = ClassFieldDef("ID", stdSizeTType, static = true)
    val guidField = ClassFieldDef("GUID", stdStringType, static = true)
    val fields = Seq(idField, guidField)
    val classDef = ClassDef(className, methods, comp.subComponents.map(cr => classNameForComponent(cr.obj.obj)), fields)
    val nsClassPrefix = s"$cppNs::$className::"
    val guidFieldInit = ClassFieldInit(s"$nsClassPrefix${guidField.name}", guidField,
      CppStringLiteral(comp.fqn.mangledNameString()))
    val idFieldInit = ClassFieldInit(s"$nsClassPrefix${idField.name}", idField,
      CppIntLiteral(componentIdByComponent.get(comp).get))
    val executeCommandImpl = ClassMethodImpl(s"$nsClassPrefix$executeCommandMethodName", commandExecuteResult,
      executeCommandParameters, CppStatements(CppSwitch(CppVar("commandId"), casesForCommands(comp),
        default = Some(CppStatements(Return(CppVar("decode::CommandExecutionResult::InvalidCommandId")))))))
    innerHNs ++= Seq(classDef, Eol)
    writeFileIfNotEmptyWithComment(hFile, protectDoubleIncludeFile(appendPrologEpilog(File(imports :+ outerHNs: _*))), s"Component ${comp.name.mangledNameString()} interface")
    writeFileIfNotEmptyWithComment(cppFile, CppFile(CppInclude(hFileName), Eol, Eol, guidFieldInit, Eol, Eol, idFieldInit, Eol, Eol, executeCommandImpl), s"Component ${comp.name.mangledNameString()} implementation")
  }

  private def casesForCommands(comp: Component): Seq[CppCase] = {
    var commandNextId = 0
    val commandsById = mutable.HashMap.empty[Int, (Component, Command)]
    comp.commands.foreach { cmd =>
      assert(commandsById.put(Option(cmd.id.orElse(null)).map(_.toInt).getOrElse({commandNextId += 1; commandNextId - 1}), (comp, cmd)).isEmpty)
    }
    subComponentsFor(comp).toSeq.sortBy(_.fqn.mangledNameString()).filterNot(_ == comp).foreach { comp =>
      comp.commands.foreach { cmd =>
        assert(commandsById.put(commandNextId, (comp, cmd)).isEmpty)
        commandNextId += 1
      }
    }
    commandsById.toSeq.sortBy(_._1).map(el => {
      CppCase(CppIntLiteral(el._1),
        CppStatements(Return(MethodCall((if (el._2._1 == comp) "" else classNameForComponent(el._2._1) + "::")
          + methodNameForDecodeName(el._2._2.name), readerVar, writerVar))))
    }: CppCase)
  }

  private def subComponentsFor(comp: Component, set: mutable.Set[Component] = mutable.HashSet.empty): mutable.Set[Component] = {
    comp.subComponents.foreach { ref =>
      val c = ref.obj.obj
      set.add(c)
      subComponentsFor(c, set)
    }
    set
  }
}
