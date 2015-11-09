package ru.mipt.acsl.decode.model.domain

import ru.mipt.acsl.decode.model.domain.impl.`type`.DecodeFqnImpl
import ru.mipt.acsl.decode.model.domain.proxy.DecodeMaybeProxy

import scala.collection.immutable.HashSet

/**
  * @author Artem Shein
  */
trait DecodeName {
  def asString(): String
}

object DecodeName {
  def mangleName(name: String): String = {
    var result = name
    if (result.startsWith("^")) {
      result = result.substring(1)
    }
    result = "[ \\\\^]".r.replaceAllIn(result, "")
    if (result.isEmpty)
      sys.error("invalid name")
    result
  }
}

trait DecodeElement

trait DecodeOptionalNameAware {
  def optionalName: Option[DecodeName]
}

trait DecodeNameAware extends DecodeOptionalNameAware {
  def name: DecodeName
}

trait DecodeOptionalInfoAware {
  def info: Option[String]
}

trait DecodeFqn {
  def parts: Seq[DecodeName]

  def asString(): String = parts.map(_.asString()).mkString(".")

  def last: DecodeName = parts.last

  def copyDropLast(): DecodeFqn

  def size: Int = parts.size

  def isEmpty: Boolean = parts.isEmpty
}

trait DecodeNamespace extends DecodeReferenceable with DecodeNameAware {
  def asString: String

  def units: Seq[DecodeUnit]

  def types: Seq[DecodeType]

  def types_= (types: Seq[DecodeType])

  def subNamespaces: Seq[DecodeNamespace]

  def parent: Option[DecodeNamespace]

  def components: Seq[DecodeComponent]

  def accept[T](visitor: DecodeReferenceableVisitor[T]): T = visitor.visit(this)

  def languages: Seq[DecodeLanguage]

  def fqn(): DecodeFqn = {
    val parts: scala.collection.mutable.Buffer[DecodeName] = scala.collection.mutable.Buffer[DecodeName]()
    var currentNamespace: DecodeNamespace = this
    while (currentNamespace.parent.isDefined) {
      parts += currentNamespace.name
      currentNamespace = currentNamespace.parent.get
    }
    parts += currentNamespace.name
    DecodeFqnImpl(parts.reverse)
  }

  def parent_=(parent: Option[DecodeNamespace])

  def rootNamespace: DecodeNamespace = parent.map(_.rootNamespace).getOrElse(this)
}

trait DecodeNamespaceAware {
  def namespace: DecodeNamespace

  def namespace_=(namespace: DecodeNamespace)
}

trait DecodeOptionalNameAndOptionalInfoAware extends DecodeOptionalInfoAware with DecodeOptionalNameAware

trait DecodeUnit extends DecodeNameAware with DecodeOptionalInfoAware with DecodeReferenceable with DecodeNamespaceAware {
  def display: Option[String]

  def accept[T] (visitor: DecodeReferenceableVisitor[T] ): T = visitor.visit(this)
}

trait DecodeReferenceableVisitor[T] {
  def visit(namespace: DecodeNamespace): T
  def visit(`type`: DecodeType): T
  def visit(component: DecodeComponent): T
  def visit(unit: DecodeUnit): T
  def visit(language: DecodeLanguage): T
}

// Types
object TypeKind extends Enumeration {
  type TypeKind = Value
  val Int, Uint, Float, Bool = Value
}

trait DecodeType extends DecodeReferenceable with DecodeOptionalNameAndOptionalInfoAware with DecodeNamespaceAware {

  import TypeKind._

  def typeKindByName(name: String): Option[TypeKind.Value] = {
    name match {
      case "int" => Some(Int)
      case "uint" => Some(Uint)
      case "float" => Some(Float)
      case "bool" => Some(Bool)
      case _ => None
    }
  }

  def nameForTypeKind(typeKind: TypeKind.Value): String = {
    typeKind match {
      case Int => "int"
      case Uint => "uint"
      case Float => "float"
      case Bool => "bool"
    }
  }

  def accept[T](visitor: DecodeTypeVisitor[T]): T

  def accept[T](visitor: DecodeReferenceableVisitor[T] ): T = visitor.visit(this)
}

trait DecodePrimitiveType extends DecodeType {
  def bitLength: Long

  def kind: TypeKind.Value

  def accept[T](visitor: DecodeTypeVisitor[T]): T = visitor.visit(this)
}

trait DecodeNativeType extends DecodeType with DecodeNameAware {
}

object DecodeNativeType {
  val MANGLED_TYPE_NAMES: Set[String] = HashSet[String]()
}

trait DecodeAliasType extends DecodeType with DecodeNameAware {
  def accept[T](visitor: DecodeTypeVisitor[T]): T = visitor.visit(this)
  def `type`: DecodeMaybeProxy[DecodeType]
}

trait DecodeSubType extends DecodeType {
  def baseType: DecodeMaybeProxy[DecodeType]

  def accept[T](visitor: DecodeTypeVisitor[T]): T = visitor.visit(this)
}

trait DecodeEnumConstant extends DecodeOptionalInfoAware {
  def name: DecodeName
  def value: String
}

trait DecodeEnumType extends DecodeType {
  def baseType: DecodeMaybeProxy[DecodeType]

  def constants: Set[DecodeEnumConstant]

  override def accept[T](visitor: DecodeTypeVisitor[T]): T = visitor.visit(this)
}

trait ArraySize {
  def minLength: Long
  def maxLength: Long
}

trait DecodeArrayType extends DecodeType {
  def size: ArraySize

  def baseType: DecodeMaybeProxy[DecodeType]

  def isFixedSize: Boolean = {
    val thisSize: ArraySize = size
    val maxLength: Long = thisSize.maxLength
    thisSize.minLength == maxLength && maxLength != 0
  }

  def accept[T](visitor: DecodeTypeVisitor[T]): T = visitor.visit(this)
}

trait DecodeStructField extends DecodeNameAware with DecodeOptionalInfoAware {
  def `type`: DecodeMaybeProxy[DecodeType]

  def unit: Option[DecodeMaybeProxy[DecodeUnit]]
}

trait DecodeStructType extends DecodeType {
  def fields: Seq[DecodeStructField]

  override def accept[T](visitor: DecodeTypeVisitor[T]): T = visitor.visit(this)
}

trait DecodeGenericType extends DecodeType {
  def typeParameters: Seq[Option[DecodeName]]

  def accept[T](visitor: DecodeTypeVisitor[T]): T = visitor.visit(this)
}

trait DecodeGenericTypeSpecialized extends DecodeType {
  def genericType: DecodeMaybeProxy[DecodeGenericType]

  def genericTypeArguments: Seq[Option[DecodeMaybeProxy[DecodeType]]]

  def accept[T] (visitor: DecodeTypeVisitor[T]): T = visitor.visit(this)
}

trait DecodeTypeVisitor[T] {
  def visit(primitiveType: DecodePrimitiveType): T
  def visit(nativeType: DecodeNativeType): T
  def visit(subType: DecodeSubType): T
  def visit(enumType: DecodeEnumType): T
  def visit(arrayType: DecodeArrayType): T
  def visit(structType: DecodeStructType): T
  def visit(typeAlias: DecodeAliasType): T
  def visit(genericType: DecodeGenericType): T
  def visit(genericTypeSpecialized: DecodeGenericTypeSpecialized): T
}

// Components
trait DecodeCommandArgument extends DecodeNameAware with DecodeOptionalInfoAware {
  def unit: Option[DecodeMaybeProxy[DecodeUnit]]
  def `type`: DecodeMaybeProxy[DecodeType]
}

trait DecodeCommand extends DecodeOptionalInfoAware with DecodeNameAware {
  def returnType: Option[DecodeMaybeProxy[DecodeType]]
  def id: Option[Int]
  def arguments: Seq[DecodeCommandArgument]
}