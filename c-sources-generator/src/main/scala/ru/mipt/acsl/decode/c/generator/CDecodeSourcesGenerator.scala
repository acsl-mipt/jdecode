package ru.mipt.acsl.decode.c.generator

import java.io._
import java.security.MessageDigest

import com.typesafe.scalalogging.LazyLogging
import ru.mipt.acsl.decode.model.domain.`type`.DecodeType.TypeKind
import ru.mipt.acsl.decode.model.domain.`type`._
import ru.mipt.acsl.decode.model.domain.{DecodeComponent, DecodeNamespace, DecodeRegistry}
import ru.mipt.acsl.generation.Generator
import ru.mipt.acsl.generator.c.ast._

import scala.collection.JavaConversions._
import resource._

import scala.collection.mutable
import scala.util.Random

class CDecodeGeneratorConfiguration(val outputDir: File, val registry: DecodeRegistry)
{

}

class CDecodeSourcesGenerator(val config: CDecodeGeneratorConfiguration) extends Generator[CDecodeGeneratorConfiguration] with LazyLogging
{
  override def generate() = {
    config.registry.getRootNamespaces.toList.foreach(generateNs)
  }

  def makeDirForNs(ns: DecodeNamespace) : File = {
    val dir = new File(config.outputDir, ns.getFqn.getParts.toList.map(_.asString()).mkString("/"))
    if(!(dir.exists() || dir.mkdirs()))
      sys.error(s"Can't create directory ${dir.getAbsolutePath}")
    dir
  }

  def generateNs(ns: DecodeNamespace): Unit = {
    val nsDir = makeDirForNs(ns)
    ns.getSubNamespaces.toTraversable.foreach(generateNs)
    val typesHeader = HFile()
    ns.getTypes.toTraversable.foreach(generateType(_).map(typesHeader += _))
    if (typesHeader.statements.nonEmpty) {
      for (typeHeaderStream <- managed(new OutputStreamWriter(new FileOutputStream(new File(nsDir, "types.h"))))) {
        protectDoubleIncludeFile(typesHeader).generate(CGeneratorState(typeHeaderStream))
      }
    }
    ns.getComponents.toTraversable.foreach(generateComponent)
  }

  var typeNameId: Int = 0

  private def cTypeNameFor(t: DecodeType): String = {
    if (t.getOptionalName.isPresent) {
      t.getOptionalName.get().asString()
    } else {
      typeNameId += 1
      "type" + typeNameId
    }
  }

  private val rand = new Random()

  private def protectDoubleIncludeFile(file: HFile): HFile = {
    val bytes = Array[Byte](10)
    rand.nextBytes(bytes)
    val uniqueName: String = "__" ++ MessageDigest.getInstance("MD5").digest(bytes).map("%02x".format(_)).mkString ++ "__"
    HFile(Seq(CIfNDef(uniqueName, Seq(CDefine(uniqueName)))) ++ file.statements ++ Seq(CEndIf()))
  }


  private val refTypeVisitor = new DecodeTypeVisitor[CType] {
    override def visit(primitiveType: DecodePrimitiveType): CType = primitiveType.getKind match {
      case TypeKind.BOOL => CTypeApplication("bool")
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

    override def visit(nativeType: DecodeNativeType): CType = CTypeApplication(cTypeNameFor(nativeType))

    override def visit(subType: DecodeSubType): CType = CTypeApplication(cTypeNameFor(subType))

    override def visit(enumType: DecodeEnumType): CType = CTypeApplication(cTypeNameFor(enumType))

    override def visit(arrayType: DecodeArrayType): CType = CTypeApplication(cTypeNameFor(arrayType))

    override def visit(structType: DecodeStructType): CType = CTypeApplication(cTypeNameFor(structType))

    override def visit(typeAlias: DecodeAliasType): CType = CTypeApplication(cTypeNameFor(typeAlias))
  }

  private def generateType(t: DecodeType): Option[HStmt] = {
    t.accept(new DecodeTypeVisitor[Option[HStmt]] {
      override def visit(primitiveType: DecodePrimitiveType): Option[HStmt] = {
        Some(CTypeDefStmt(cTypeNameFor(primitiveType), primitiveType.accept(refTypeVisitor)))
      }

      override def visit(nativeType: DecodeNativeType): Option[HStmt] = Some(CTypeDefStmt(cTypeNameFor(t), CVoidType.ptr()))

      override def visit(subType: DecodeSubType): Option[HStmt] = Some(CTypeDefStmt(cTypeNameFor(t), subType.getBaseType.getObject.accept(refTypeVisitor)))

      override def visit(enumType: DecodeEnumType): Option[HStmt] = Some(CTypeDefStmt(cTypeNameFor(t),
        CEnumTypeDef(enumType.getConstants.toIterable.map(c => CEnumTypeDefConst(c.getName.asString(), c.getValue.toInt)))))

      override def visit(arrayType: DecodeArrayType): Option[HStmt] = Some(CTypeDefStmt(cTypeNameFor(t), CVoidType.ptr()))

      override def visit(structType: DecodeStructType): Option[HStmt] = Some(CTypeDefStmt(cTypeNameFor(t),
        CStructTypeDef(structType.getFields.toIterable.map(f => CStructTypeDefField(f.getName.asString(), f.getType.getObject.accept(refTypeVisitor))))))

      override def visit(typeAlias: DecodeAliasType): Option[HStmt] = Some(CDefine(cTypeNameFor(t), cTypeNameFor(typeAlias.getType.getObject)))
    })

  }

  private def generateComponent(comp: DecodeComponent) = {
    logger.debug(s"Generating component ${comp.getName.asString()}")
  }

  override def getConfiguration: CDecodeGeneratorConfiguration = config
}

object CDecodeSourcesGenerator {

}