package ru.mipt.acsl.decode.c.generator

import java.io._
import java.security.MessageDigest
import java.util.Optional

import com.typesafe.scalalogging.LazyLogging
import ru.mipt.acsl.decode.model.domain.`type`.DecodeType.TypeKind
import ru.mipt.acsl.decode.model.domain.`type`._
import ru.mipt.acsl.decode.model.domain._
import ru.mipt.acsl.generation.Generator
import ru.mipt.acsl.generator.c.ast._

import scala.collection.JavaConversions._
import resource._

import scala.collection.mutable
import scala.util.Random

class CDecodeGeneratorConfiguration(val outputDir: File, val registry: DecodeRegistry,
                                    val namespaceAlias: Map[DecodeFqn, DecodeFqn] = Map())
{

}

class CDecodeSourcesGenerator(val config: CDecodeGeneratorConfiguration) extends Generator[CDecodeGeneratorConfiguration] with LazyLogging
{
  override def generate() = {
    config.registry.getRootNamespaces.toList.foreach(generateNs)
  }

  def dirForNs(ns: DecodeNamespace): File = {
    new File(config.outputDir,
      config.namespaceAlias.getOrElse(ns.getFqn, ns.getFqn).getParts.toList.map(_.asString()).mkString("/"))
  }

  def makeDirForNs(ns: DecodeNamespace): File = {
    val dir = dirForNs(ns)
    if(!(dir.exists() || dir.mkdirs()))
      sys.error(s"Can't create directory ${dir.getAbsolutePath}")
    dir
  }

  def writeFile[T <: CStmt](file: File, hfile: CFile[T]): Unit = {
    for (typeHeaderStream <- managed(new OutputStreamWriter(new FileOutputStream(file)))) {
      hfile.generate(CGeneratorState(typeHeaderStream))
    }
  }

  def writeFileIfNotEmptyWithComment[A >: HStmt <: CStmt](file: File, cFile: CFile[A], comment: String): Unit = {
    if (cFile.statements.nonEmpty) {
      cFile.statements.prepend(HComment(comment), HEol)
      writeFile(file, cFile)
    }
  }

  def generateNs(ns: DecodeNamespace): Unit = {
    val nsDir = makeDirForNs(ns)
    ns.getSubNamespaces.toTraversable.foreach(generateNs)
    val typesHeader = HFile()
    ns.getTypes.toTraversable.foreach(t => {
      generateType(t).map({
        if (t.getOptionalName.isPresent)
          typesHeader ++= Seq(HComment(t.getOptionalName.get().asString()), HEol)
        typesHeader ++= Seq(_, HEol)
      })
    })
    writeFileIfNotEmptyWithComment(new File(nsDir, "types.h"), protectDoubleIncludeFile(typesHeader), s"Types of ${ns.getFqn.asString()} namespace")
    ns.getComponents.toTraversable.foreach(generateComponent)
  }

  var typeNameId: Int = 0

  private val typeNameVisitor: DecodeTypeVisitor[String] = new DecodeTypeVisitor[String] {
    override def visit(primitiveType: DecodePrimitiveType): String = primitiveTypeToCTypeApplication(primitiveType).name

    override def visit(nativeType: DecodeNativeType): String = nativeType.getName.asString()

    override def visit(subType: DecodeSubType): String = cTypeNameFromOptionalName(subType.getOptionalName)

    override def visit(enumType: DecodeEnumType): String = cTypeNameFromOptionalName(enumType.getOptionalName)

    override def visit(arrayType: DecodeArrayType): String = {
      val baseCType: String = arrayType.getBaseType.getObject.accept(typeNameVisitor)
      val min = arrayType.getSize.getMinLength
      val max = arrayType.getSize.getMaxLength
      "DECODE_ARRAY_TYPE_" + ((arrayType.isFixedSize, min, max) match {
        case (true, 0, _) | (false, 0, 0) => s"NAME($baseCType)"
        case (true, _, _) => s"FIXED_SIZE_NAME($baseCType, $min)"
        case (false, 0, _) => s"MAX_NAME($baseCType, $max)"
        case (false, _, 0) => s"MIN_NAME($baseCType, $min)"
        case (false, _, _) => s"MIN_MAX_NAME($baseCType, $min, $max)"
      })
    }

    override def visit(structType: DecodeStructType): String = cTypeNameFromOptionalName(structType.getOptionalName)

    override def visit(typeAlias: DecodeAliasType): String = typeAlias.getName.asString()
  }

  private def cTypeNameFromOptionalName(name: Optional[DecodeName]): String = {
    if (name.isPresent) {
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
    primitiveType.getKind match {
      case TypeKind.BOOL => primitiveType.getBitLength match {
        case 8 => CTypeApplication("b8")
        case 16 => CTypeApplication("b16")
        case 32 => CTypeApplication("b32")
        case 64 => CTypeApplication("b64")
        case _ => sys.error("illegal bit length")
      }
      case TypeKind.FLOAT => primitiveType.getBitLength match {
        case 32 => CFloatType
        case 64 => CDoubleType
        case _ => sys.error("illegal bit length")
      }
      case TypeKind.INT => primitiveType.getBitLength match {
        case 8 => CUnsignedCharType
        case 16 => CUnsignedShortType
        case 32 => CUnsignedIntType
        case 64 => CUnsignedLongType
        case _ => sys.error("illegal bit length")
      }
      case TypeKind.UINT => primitiveType.getBitLength match {
        case 8 => CSignedCharType
        case 16 => CSignedShortType
        case 32 => CSignedIntType
        case 64 => CSignedLongType
        case _ => sys.error("illegal bit length")
      }
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
  }

  private def generateType(t: DecodeType): Option[HStmt] = {
    t.accept(new DecodeTypeVisitor[Option[HStmt]] {
      override def visit(primitiveType: DecodePrimitiveType) = {
        Some(CTypeDefStmt(cTypeNameFor(primitiveType), primitiveType.accept(refTypeVisitor)))
      }

      override def visit(nativeType: DecodeNativeType) = Some(CTypeDefStmt(cTypeNameFor(t), CVoidType.ptr()))

      override def visit(subType: DecodeSubType) = Some(CTypeDefStmt(cTypeNameFor(t), subType.getBaseType.getObject.accept(refTypeVisitor)))

      override def visit(enumType: DecodeEnumType) = Some(CTypeDefStmt(cTypeNameFor(t),
        CEnumTypeDef(enumType.getConstants.toIterable.map(c => CEnumTypeDefConst(c.getName.asString(), c.getValue.toInt)))))

      override def visit(arrayType: DecodeArrayType) = Some(CTypeDefStmt(cTypeNameFor(t), CVoidType.ptr()))

      override def visit(structType: DecodeStructType) = Some(CTypeDefStmt(cTypeNameFor(t),
        CStructTypeDef(structType.getFields.toIterable.map(f => CStructTypeDefField(f.getName.asString(), f.getType.getObject.accept(refTypeVisitor))))))

      override def visit(typeAlias: DecodeAliasType) = {
        val newName: String = cTypeNameFor(t)
        val oldName: String = cTypeNameFor(typeAlias.getType.getObject)
        if (newName equals oldName) Some(HComment("omitted due name clash")) else Some(CDefine(newName, oldName))
      }
    })

  }

  private def generateComponent(comp: DecodeComponent) = {
    logger.debug(s"Generating component ${comp.getName.asString()}")
    val dir = dirForNs(comp.getNamespace)
    val hFile = new File(dir, comp.getName.asString() + "Component.h")
    val cFile = new File(dir, comp.getName.asString() + "Component.c")
    writeFileIfNotEmptyWithComment(hFile, protectDoubleIncludeFile(HFile()), "Component header")
    writeFileIfNotEmptyWithComment(cFile, CFile(HEol), "Component header")
  }

  override def getConfiguration: CDecodeGeneratorConfiguration = config
}

object CDecodeSourcesGenerator {

}