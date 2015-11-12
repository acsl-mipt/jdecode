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

  def dirForNs(ns: DecodeNamespace): File = {
    new File(config.outputDir,
      config.namespaceAlias.getOrElse(ns.fqn, ns.fqn).parts.toList.map(_.asString()).mkString("/"))
  }

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
    ns.types.toTraversable.foreach(t => {
      generateType(t).map({
        if (t.optionalName.isDefined)
          typesHeader ++= Seq(HComment(t.optionalName.get.asString()), HEol)
        typesHeader ++= Seq(_, HEol)
      })
    })
    writeFileIfNotEmptyWithComment(new File(nsDir, "types.h"), protectDoubleIncludeFile(typesHeader), s"Types of ${ns.fqn.asString()} namespace")
    //ns.getComponents.toTraversable.foreach(generateRootComponent)
  }

  var typeNameId: Int = 0

  private val typeNameVisitor: DecodeTypeVisitor[String] = new DecodeTypeVisitor[String] {
    override def visit(primitiveType: DecodePrimitiveType): String = primitiveTypeToCTypeApplication(primitiveType).name

    override def visit(nativeType: DecodeNativeType): String = nativeType.name.asString()

    override def visit(subType: DecodeSubType): String = cTypeNameFromOptionalName(subType.optionalName)

    override def visit(enumType: DecodeEnumType): String = cTypeNameFromOptionalName(enumType.optionalName)

    override def visit(arrayType: DecodeArrayType): String = {
      val baseCType: String = arrayType.baseType.obj.accept(typeNameVisitor)
      val min = arrayType.size.minLength
      val max = arrayType.size.maxLength
      "DECODE_ARRAY_TYPE_" + ((arrayType.isFixedSize, min, max) match {
        case (true, 0, _) | (false, 0, 0) => s"NAME($baseCType)"
        case (true, _, _) => s"FIXED_SIZE_NAME($baseCType, $min)"
        case (false, 0, _) => s"MAX_NAME($baseCType, $max)"
        case (false, _, 0) => s"MIN_NAME($baseCType, $min)"
        case (false, _, _) => s"MIN_MAX_NAME($baseCType, $min, $max)"
      })
    }

    override def visit(structType: DecodeStructType): String = cTypeNameFromOptionalName(structType.optionalName)

    override def visit(typeAlias: DecodeAliasType): String = typeAlias.name.asString()

    override def visit(genericType: DecodeGenericType): String = cTypeNameFromOptionalName(genericType.optionalName)

    override def visit(genericTypeSpecialized: DecodeGenericTypeSpecialized): String =
      cTypeNameFor(genericTypeSpecialized.genericType.obj) + "_" +
        genericTypeSpecialized.genericTypeArguments.to[Iterable].map(tp => if (tp.isDefined) cTypeNameFor(tp.get.obj) else "void").mkString("_")
  }

  private def cTypeNameFromOptionalName(name: Option[DecodeName]): String = {
    if (name.isDefined) {
      name.get.asString()
    } else {
      typeNameId += 1
      "type" + typeNameId
    }
  }

  private def cTypeNameFor(t: DecodeType): String = {
    t.accept(typeNameVisitor)
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

  private val refTypeVisitor = new DecodeTypeVisitor[CTypeApplication] {
    override def visit(primitiveType: DecodePrimitiveType): CTypeApplication = primitiveTypeToCTypeApplication(primitiveType)

    override def visit(nativeType: DecodeNativeType) = CTypeApplication(cTypeNameFor(nativeType))

    override def visit(subType: DecodeSubType) = CTypeApplication(cTypeNameFor(subType))

    override def visit(enumType: DecodeEnumType) = CTypeApplication(cTypeNameFor(enumType))

    override def visit(arrayType: DecodeArrayType) = CTypeApplication(cTypeNameFor(arrayType))

    override def visit(structType: DecodeStructType) = CTypeApplication(cTypeNameFor(structType))

    override def visit(typeAlias: DecodeAliasType) = CTypeApplication(cTypeNameFor(typeAlias))

    override def visit(genericType: DecodeGenericType): CTypeApplication = CTypeApplication(cTypeNameFor(genericType))

    override def visit(genericTypeSpecialized: DecodeGenericTypeSpecialized): CTypeApplication = CTypeApplication(cTypeNameFor(genericTypeSpecialized))
  }

  private def generateType(t: DecodeType): Option[HStmt] = {
    t.accept(new DecodeTypeVisitor[Option[HStmt]] {
      override def visit(primitiveType: DecodePrimitiveType) = {
        Some(CTypeDefStmt(cTypeNameFor(primitiveType), primitiveType.accept(refTypeVisitor)))
      }

      override def visit(nativeType: DecodeNativeType) = Some(CTypeDefStmt(cTypeNameFor(t), CVoidType.ptr()))

      override def visit(subType: DecodeSubType) = Some(CTypeDefStmt(cTypeNameFor(t), subType.baseType.obj.accept(refTypeVisitor)))

      override def visit(enumType: DecodeEnumType) = Some(CTypeDefStmt(cTypeNameFor(t),
        CEnumTypeDef(enumType.constants.toIterable.map(c => CEnumTypeDefConst(c.name.asString(), c.value.toInt)))))

      override def visit(arrayType: DecodeArrayType) = Some(CTypeDefStmt(cTypeNameFor(t), CVoidType.ptr()))

      override def visit(structType: DecodeStructType) = Some(CTypeDefStmt(cTypeNameFor(t),
        CStructTypeDef(structType.fields.toIterable.map(f => CStructTypeDefField(f.name.asString(), f.fieldType.obj.accept(refTypeVisitor))))))

      override def visit(typeAlias: DecodeAliasType) = {
        val newName: String = cTypeNameFor(t)
        val oldName: String = cTypeNameFor(typeAlias.baseType.obj)
        if (newName equals oldName) Some(HComment("omitted due name clash")) else Some(CDefine(newName, oldName))
      }

      override def visit(genericType: DecodeGenericType): Option[HStmt] = sys.error("not implemented")

      override def visit(genericTypeSpecialized: DecodeGenericTypeSpecialized): Option[HStmt] = sys.error("not implemented")
    })

  }

  private def collectNsForType[T <: DecodeType](t: DecodeMaybeProxy[T], set: mutable.Set[DecodeNamespace]) {
    require(t.isResolved, s"Proxy not resolved error for ${t.proxy.toString}")
    collectNsForType(t.obj, set)
  }

  private def collectNsForType(t: DecodeType, set: mutable.Set[DecodeNamespace]) {
    set += t.namespace
    t.accept(new DecodeTypeVisitor[Unit] {
      override def visit(primitiveType: DecodePrimitiveType) {}

      override def visit(nativeType: DecodeNativeType) {}

      override def visit(subType: DecodeSubType) = collectNsForType(subType.baseType, set)

      override def visit(enumType: DecodeEnumType) = collectNsForType(enumType.baseType, set)

      override def visit(arrayType: DecodeArrayType) = collectNsForType(arrayType.baseType, set)

      override def visit(structType: DecodeStructType) = structType.fields.to[Iterable].foreach(f => collectNsForType(f.fieldType, set))

      override def visit(typeAlias: DecodeAliasType) = collectNsForType(typeAlias.baseType, set)

      override def visit(genericType: DecodeGenericType) {}

      override def visit(genericTypeSpecialized: DecodeGenericTypeSpecialized) = genericTypeSpecialized.genericTypeArguments.filter(_.isDefined).foreach(a => collectNsForType(a.get, set))
    })
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