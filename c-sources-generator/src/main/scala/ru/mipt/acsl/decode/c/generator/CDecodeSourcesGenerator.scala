package ru.mipt.acsl.decode.c.generator

import java.io._
import java.security.MessageDigest
import java.util.Optional

import com.typesafe.scalalogging.LazyLogging
import ru.mipt.acsl.decode.model.domain.TypeKind
import ru.mipt.acsl.decode.model.domain._
import ru.mipt.acsl.generation.Generator
import ru.mipt.acsl.generator.c.ast._

import resource._

import scala.collection.mutable
import scala.util.Random

class CDecodeGeneratorConfiguration(val outputDir: File, val registry: DecodeRegistry, val rootComponentFqn: String,
                                    val namespaceAlias: Map[DecodeFqn, DecodeFqn] = Map()) {

}

class CDecodeSourcesGenerator(val config: CDecodeGeneratorConfiguration) extends Generator[CDecodeGeneratorConfiguration] with LazyLogging {
  override def generate() {
    generateRootComponent(config.registry.getComponent(config.rootComponentFqn).get)
  }

  def dirForNs(ns: DecodeNamespace): File = new File(config.outputDir,
      config.namespaceAlias.getOrElse(ns.fqn, ns.fqn).parts.toList.map(_.asString()).mkString("/"))

  def ensureDirForNsExists(ns: DecodeNamespace): File = {
    val dir = dirForNs(ns)
    if (!(dir.exists() || dir.mkdirs()))
      sys.error(s"Can't create directory ${dir.getAbsolutePath}")
    dir
  }

  def writeFile[T <: CStmt](file: File, hfile: CFile[T]) {
    for (typeHeaderStream <- managed(new OutputStreamWriter(new FileOutputStream(file)))) {
      hfile.generate(CGeneratorState(typeHeaderStream))
    }
  }

  def writeFileIfNotEmptyWithComment[A >: HStmt <: CStmt](file: File, cFile: CFile[A], comment: String) {
    if (cFile.statements.nonEmpty) {
      cFile.statements.prepend(HComment(comment), HEol)
      writeFile(file, cFile)
    }
  }

  def generateNs(ns: DecodeNamespace) {
    val nsDir = ensureDirForNsExists(ns)
    ns.subNamespaces.toTraversable.foreach(generateNs)
    val typesHeader = HFile()
    ns.types.toTraversable.foreach(generateType(_, nsDir))
    writeFileIfNotEmptyWithComment(new File(nsDir, "types.h"), protectDoubleIncludeFile(typesHeader), s"Types of ${ns.fqn.asString()} namespace")
    //ns.getComponents.toTraversable.foreach(generateRootComponent)
  }

  var fileNameId: Int = 0
  var typeNameId: Int = 0

  private def fileNameFromOptionName(name: Option[DecodeName]): String = {
    if (name.isDefined) {
      name.get.asString()
    } else {
      fileNameId += 1
      "type" + fileNameId
    }
  }

  private def cTypeNameFromOptionName(name: Option[DecodeName]): String = {
    if (name.isDefined) {
      name.get.asString()
    } else {
      typeNameId += 1
      "type" + typeNameId
    }
  }

  private def fileNameFor(t: DecodeType): String = t match {
    case t: DecodeNamed => t.name.asString()
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
      case t: DecodeNamed => t.name.asString()
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

  private def protectDoubleIncludeFile(file: HFile): HFile = {
    val bytes = Array[Byte](10)
    rand.nextBytes(bytes)
    val uniqueName: String = "__" ++ MessageDigest.getInstance("MD5").digest(bytes).map("%02x".format(_)).mkString ++ "__"
    HFile(Seq(CIfNDef(uniqueName, Seq(CDefine(uniqueName)))) ++ file.statements ++ Seq(CEndIf()): _*)
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

  def cTypeDefForName(t: DecodeType, cType: CType) = CTypeDefStmt(cTypeNameFor(t), cType)
  def cTypeAppForTypeName(t: DecodeType): CTypeApplication = CTypeApplication(cTypeNameFor(t))

  private def generateType(t: DecodeType, nsDir: File) {
    if (t.isInstanceOf[DecodePrimitiveType])
      return
    val tFile = new File(nsDir, fileNameFor(t) + ".h")
    writeFileIfNotEmptyWithComment(tFile, HFile(
    t match {
      case t: DecodePrimitiveType => cTypeDefForName(t, cTypeAppForTypeName(t))
      case t: DecodeNativeType => cTypeDefForName(t, CVoidType.ptr())
      case t: DecodeSubType => cTypeDefForName(t, cTypeAppForTypeName(t.baseType.obj))
      case t: DecodeEnumType => cTypeDefForName(t,
        CEnumTypeDef(t.constants.map(c => CEnumTypeDefConst(c.name.asString(), c.value.toInt))))
      case t: DecodeArrayType => cTypeDefForName(t, CVoidType.ptr())
      case t: DecodeStructType => cTypeDefForName(t, CStructTypeDef(t.fields.map(f => CStructTypeDefField(f.name.asString(), cTypeAppForTypeName(f.fieldType.obj)))))
      case t: DecodeAliasType =>
        val newName: String = cTypeNameFor(t)
        val oldName: String = cTypeNameFor(t.baseType.obj)
        if (newName equals oldName) HComment("omitted due name clash") else CDefine(newName, oldName)
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
      case baseTyped: BaseTyped => collectNsForType(baseTyped.baseType, set)
      case structType: DecodeStructType => structType.fields.foreach(f => collectNsForType(f.fieldType, set))
      case genericTypeSpecialized: DecodeGenericTypeSpecialized => genericTypeSpecialized.genericTypeArguments
        .filter(_.isDefined).foreach(a => collectNsForType(a.get, set))
      case _ =>
    }
  }

  private def collectNsForTypes(comp: DecodeComponent, set: mutable.Set[DecodeNamespace]) {
    comp.subComponents.foreach(cr => collectNsForTypes(cr.component.obj, set))
    if (comp.baseType.isDefined)
      collectNsForType(comp.baseType.get, set)
    comp.commands.foreach(cmd => {
      cmd.arguments.foreach(arg => collectNsForType(arg.argType, set))
      if (cmd.returnType.isDefined)
        collectNsForType(cmd.returnType.get, set)
    })
  }

  private def generateRootComponent(comp: DecodeComponent) {
    logger.debug(s"Generating component ${comp.name.asString()}")
    val nsSet = mutable.HashSet[DecodeNamespace]()
    collectNsForTypes(comp, nsSet)
    nsSet.foreach(generateNs)
    generateComponent(comp)
  }

  private def generateComponent(comp: DecodeComponent) {
    val dir = dirForNs(comp.namespace)
    val hFile = new File(dir, comp.name.asString() + "Component.h")
    val cFile = new File(dir, comp.name.asString() + "Component.c")
    writeFileIfNotEmptyWithComment(hFile, protectDoubleIncludeFile(HFile()), "Component header")
    writeFileIfNotEmptyWithComment(cFile, CFile(HEol), "Component header")
  }

  override def getConfiguration: CDecodeGeneratorConfiguration = config
}

object CDecodeSourcesGenerator {

}