package ru.mipt.acsl.decode.model.domain

import java.net.URI

import ru.mipt.acsl.decode.model.domain.impl.`type`.DecodeParameterWalker
import ru.mipt.acsl.decode.model.domain.impl.`type`.{AbstractDecodeOptionalInfoAware, DecodeFqnImpl}
import ru.mipt.acsl.decode.model.domain.impl.{DecodeComponentWalker, DecodeNameImpl}

import scala.collection.immutable.HashSet
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

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

  def units: mutable.Buffer[DecodeUnit]

  def types: mutable.Buffer[DecodeType]

  def types_= (types: mutable.Buffer[DecodeType])

  def subNamespaces: mutable.Buffer[DecodeNamespace]

  def parent: Option[DecodeNamespace]

  def components: mutable.Buffer[DecodeComponent]

  def accept[T](visitor: DecodeReferenceableVisitor[T]): T = visitor.visit(this)

  def languages: mutable.Buffer[DecodeLanguage]

  def fqn: DecodeFqn = {
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
}

trait DecodeType extends DecodeReferenceable with DecodeOptionalNameAndOptionalInfoAware with DecodeNamespaceAware {
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

trait BaseTyped {
  def baseType: DecodeMaybeProxy[DecodeType]
}

trait DecodeAliasType extends DecodeType with DecodeNameAware with BaseTyped {
  def accept[T](visitor: DecodeTypeVisitor[T]): T = visitor.visit(this)
}

trait DecodeSubType extends DecodeType with BaseTyped {
  def accept[T](visitor: DecodeTypeVisitor[T]): T = visitor.visit(this)
}

trait DecodeEnumConstant extends DecodeOptionalInfoAware {
  def name: DecodeName
  def value: String
}

trait DecodeEnumType extends DecodeType with BaseTyped {
  def constants: mutable.Set[DecodeEnumConstant]

  override def accept[T](visitor: DecodeTypeVisitor[T]): T = visitor.visit(this)
}

trait ArraySize {
  def minLength: Long
  def maxLength: Long
}

trait DecodeArrayType extends DecodeType with BaseTyped {
  def size: ArraySize

  def isFixedSize: Boolean = {
    val thisSize: ArraySize = size
    val maxLength: Long = thisSize.maxLength
    thisSize.minLength == maxLength && maxLength != 0
  }

  def accept[T](visitor: DecodeTypeVisitor[T]): T = visitor.visit(this)
}

trait DecodeStructField extends DecodeNameAware with DecodeOptionalInfoAware {
  def fieldType: DecodeMaybeProxy[DecodeType]

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
  def argType: DecodeMaybeProxy[DecodeType]
}

trait DecodeCommand extends DecodeOptionalInfoAware with DecodeNameAware {
  def returnType: Option[DecodeMaybeProxy[DecodeType]]
  def id: Option[Int]
  def arguments: Seq[DecodeCommandArgument]
}

// TODO: replace with case classes?
trait DecodeMessageVisitor[T] {
  def visit(eventMessage: DecodeEventMessage): T
  def visit(statusMessage: DecodeStatusMessage): T
}

trait DecodeMessage extends DecodeOptionalInfoAware with DecodeNameAware {
  def accept[T](visitor: DecodeMessageVisitor[T] ): T

  def parameters: Seq[DecodeMessageParameter]

  def component: DecodeComponent

  def id: Option[Int]
}

trait DecodeStatusMessage extends DecodeMessage {
  def accept[T](visitor: DecodeMessageVisitor[T]): T = visitor.visit(this)
}

abstract class AbstractDecodeMessage(info: Option[String]) extends AbstractDecodeOptionalInfoAware(info)
with DecodeMessage

trait DecodeComponentRef {
  def component: DecodeMaybeProxy[DecodeComponent]

  def alias: Option[String]
}

trait DecodeMessageParameter {
  def value: String
}

trait DecodeEventMessage extends DecodeMessage {
  def accept[T](visitor: DecodeMessageVisitor[T]): T = visitor.visit(this)
}

trait DecodeComponent extends DecodeOptionalInfoAware with DecodeNameAware with DecodeReferenceable
  with DecodeNamespaceAware {

  def messages: Seq[DecodeMessage]
  def commands: Seq[DecodeCommand]
  def baseType: Option[DecodeMaybeProxy[DecodeStructType]]
  def subComponents: Seq[DecodeComponentRef]
  def id: Option[Int]
  override def accept[T](visitor: DecodeReferenceableVisitor[T]): T = visitor.visit(this)
  def getTypeForParameter(parameter: DecodeMessageParameter): DecodeType = {
    val walker = new DecodeParameterWalker(parameter)
    val componentWalker = new DecodeComponentWalker(this)
    while (walker.hasNext)
    {
      val token = walker.next
      componentWalker.walk(token)
    }
    if (componentWalker.`type`.isEmpty)
      sys.error("fail")
    componentWalker.`type`.get
  }
}

trait DecodeDomainModelResolver {
  def resolve(decodeRegistry: DecodeRegistry): DecodeResolvingResult[DecodeReferenceable]
}

trait DecodeRegistry {
  def rootNamespaces: mutable.Buffer[DecodeNamespace]

  def resolve[T <: DecodeReferenceable](uri: URI, cls: Class[T]): DecodeResolvingResult[T]

  def getComponent(fqn: String): Option[DecodeComponent] = {
    val dotPos = fqn.lastIndexOf('.')
    val namespaceOptional = getNamespace(fqn.substring(0, dotPos))
    if (namespaceOptional.isEmpty)
    {
      return Option.empty
    }
    val componentName = DecodeNameImpl.newFromMangledName(fqn.substring(dotPos + 1, fqn.length()))
    namespaceOptional.get.components.find(_.name == componentName)
  }

  def getNamespace(fqn: String): Option[DecodeNamespace] = {
    var currentNamespaces: Option[ArrayBuffer[DecodeNamespace]] = Some(new ArrayBuffer[DecodeNamespace]())
    currentNamespaces.get ++= rootNamespaces
    var currentNamespace = Option.empty[DecodeNamespace]
    "\\.".r.split(fqn).foreach(nsName => {
      if (currentNamespaces.isEmpty)
      {
        return Option.empty
      }
      val decodeName = DecodeNameImpl.newFromMangledName(nsName)
      currentNamespace = currentNamespaces.get.find(_.name == decodeName)
      if (currentNamespace.isDefined)
      {
        currentNamespaces.get.clear()
        currentNamespaces.get ++= currentNamespace.get.subNamespaces
      }
      else
      {
        currentNamespaces = Option.empty[ArrayBuffer[DecodeNamespace]]
      }
    })
    currentNamespace
  }

  def getMessage(fqn: String): Option[DecodeMessage] = {
    val dotPos = fqn.lastIndexOf('.')
    val decodeName = DecodeNameImpl.newFromMangledName(fqn.substring(dotPos + 1, fqn.length()))
    getComponent(fqn.substring(0, dotPos)).map(_.messages.find(_.name == decodeName).orNull)
  }

  def getMessageOrThrow(fqn: String): DecodeMessage = {
    getMessage(fqn).get
  }
}
