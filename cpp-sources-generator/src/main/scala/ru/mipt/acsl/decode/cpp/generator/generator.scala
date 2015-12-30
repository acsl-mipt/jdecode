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
import scala.util.Random

class CppGeneratorConfiguration(val outputDir: io.File, val registry: DecodeRegistry, val rootComponentFqn: String,
                                val namespaceAlias: Map[DecodeFqn, DecodeFqn] = Map())

class CppSourcesGenerator(val config: CppGeneratorConfiguration) extends Generator[CppGeneratorConfiguration] with LazyLogging {
  override def generate() {
    generateRootComponent(config.registry.getComponent(config.rootComponentFqn).get)
  }

  def nsOrAliasCppSourceParts(ns: DecodeNamespace): Seq[String] = config.namespaceAlias.getOrElse(ns.fqn, ns.fqn).parts.map(_.asMangledString)

  def dirForNs(ns: DecodeNamespace): io.File = new io.File(config.outputDir, nsOrAliasCppSourceParts(ns).mkString("/"))

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

  def writeFileIfNotEmptyWithComment[A >: Statement <: CppStatement](file: io.File, cppFile: CppFile.Type, comment: String) {
    if (cppFile.nonEmpty) {
      writeFile(file, CppFile(cppFile.statements ++ Seq(Comment(comment), Eol.asInstanceOf[Statement])))
    }
  }

  def generateNs(ns: DecodeNamespace) {
    val nsDir = ensureDirForNsExists(ns)
    ns.subNamespaces.toTraversable.foreach(generateNs)
    val typesHeader = File()
    ns.types.toTraversable.foreach(generateType(_, nsDir))
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

  private def cppTypeNameFor(t: DecodeType): String = {
    t match {
      case t: DecodeNamed => t.name.asMangledString
      case t: DecodePrimitiveType => primitiveTypeToCTypeApplication(t).name
      case t: DecodeArrayType =>
        val baseCType: String = cppTypeNameFor(t.baseType.obj)
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
        cppTypeNameFor(t.genericType.obj) + "_" +
        t.genericTypeArguments.map(tp => if (tp.isDefined) cppTypeNameFor(tp.get.obj) else "void").mkString("_")
      case t: DecodeOptionNamed => cTypeNameFromOptionName(t.optionName)
      case _ => sys.error("not implemented")
    }
  }

  private val rand = new Random()

  private def protectDoubleIncludeFile(file: File.Type): File.Type = {
    val bytes = Array[Byte](10)
    rand.nextBytes(bytes)
    val uniqueName: String = "__" ++ MessageDigest.getInstance("MD5").digest(bytes).map("%02x".format(_)).mkString ++ "__"
    File(Seq(CppIfNDef(uniqueName, Seq(CppDefine(uniqueName)))) ++ file.statements ++ Seq(CppEndIf()): _*)
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

  private def generateType(t: DecodeType, nsDir: io.File) {
    if (t.isInstanceOf[DecodePrimitiveType])
      return
    val tFile = new io.File(nsDir, fileNameFor(t) + ".h")
    writeFileIfNotEmptyWithComment(tFile, File(
    t match {
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
    }), "Type header")
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

  def cppTypeForDecodeType(t: DecodeType): Type = {
    t.optionName.map{on => Type(on.asMangledString)}.getOrElse(Type("<not implemented>"))
  }

  private def classNameForComponent(comp: DecodeComponent): String = comp.name.asMangledString + "Component"

  private def generateComponent(comp: DecodeComponent) {
    val dir = dirForNs(comp.namespace)
    val className = classNameForComponent(comp)
    val hFileName = className + ".h"
    val hFile = new io.File(dir, hFileName)
    val cppFile = new io.File(dir, comp.name.asMangledString + "Component.cpp")
    var hNs = nsOrAliasCppSourceParts(comp.namespace).foldLeft[Option[Namespace.Type]](None)(
      (hNs: Option[Namespace.Type], part: String)
      => hNs.map{ns => Namespace(part, Seq(ns))}.orElse(Some(Namespace(part)))).get
    val methods = comp.commands.map{cmd => ClassMethodDef(cmd.name.asMangledString,
      cmd.returnType.map{rt => cppTypeForDecodeType(rt.obj)}.getOrElse(Type("void")),
      cmd.parameters.map{p => Parameter(p.name.asMangledString, cppTypeForDecodeType(p.paramType.obj))})}
    methods ++= comp.messages.map{msg =>
      ClassMethodDef("write" + msg.name.asMangledString.capitalize, Type("void"),
        Seq(Parameter("writer", Type("Writer"))))}
    methods ++= comp.baseType.map{bt => bt.obj.fields.map{f =>
      ClassMethodDef(f.name.asMangledString, cppTypeForDecodeType(f.typeUnit.t.obj), Seq.empty)}}
      .getOrElse(Seq.empty)
    hNs = Namespace(hNs.name, hNs.statements ++ Seq(ClassDef(className, methods, comp.subComponents.map{ cr => classNameForComponent(cr.component.obj)}), Eol))
    writeFileIfNotEmptyWithComment(hFile, protectDoubleIncludeFile(File(hNs)), "Component header")
    writeFileIfNotEmptyWithComment(cppFile, CppFile(Seq(CppInclude(hFileName))), "Component header")
  }

  override def getConfiguration: CppGeneratorConfiguration = config
}

object CppSourcesGenerator {

}