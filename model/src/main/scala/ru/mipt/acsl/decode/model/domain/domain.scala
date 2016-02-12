package ru.mipt.acsl.decode.model.domain

import java.net.URI

import ru.mipt.acsl.decode.model.domain.impl.`type`._
import ru.mipt.acsl.decode.model.domain.impl.{DecodeMessageParameterRefWalker, DecodeNameImpl}

import scala.collection.immutable

object DecodeConstants {
  val SYSTEM_NAMESPACE_FQN: DecodeFqn = DecodeFqnImpl.newFromSource("decode")
}

package object aliases {
  type DecodeMessageParameterToken = Either[String, Int]
}

import aliases._

trait DecodeName {
  def asMangledString: String
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

trait DecodeOptionNamed {
  def optionName: Option[DecodeName]
}

trait DecodeNamed extends DecodeOptionNamed {
  def name: DecodeName
}

trait DecodeHasOptionInfo {
  def info: Option[String]
}

trait DecodeHasOptionId {
  def id: Option[Int]
}

trait DecodeFqn {
  def parts: Seq[DecodeName]

  def asMangledString: String = parts.map(_.asMangledString).mkString(".")

  def last: DecodeName = parts.last

  def copyDropLast(): DecodeFqn

  def size: Int = parts.size

  def isEmpty: Boolean = parts.isEmpty
}

trait DecodeReferenceable extends DecodeOptionNamed {
  def accept[T](visitor: DecodeReferenceableVisitor[T]): T
}

trait DecodeLanguage extends DecodeReferenceable with DecodeNamespaceAware

trait DecodeNamespace extends DecodeReferenceable with DecodeNamed {
  def asString: String

  def units: immutable.Seq[DecodeUnit]

  def units_=(units: immutable.Seq[DecodeUnit])

  def types: immutable.Seq[DecodeType]

  def types_=(types: immutable.Seq[DecodeType])

  def subNamespaces: immutable.Seq[DecodeNamespace]

  def subNamespaces_=(namespaces: immutable.Seq[DecodeNamespace])

  def parent: Option[DecodeNamespace]

  def parent_=(parent: Option[DecodeNamespace])

  def components: immutable.Seq[DecodeComponent]

  def components_=(components: immutable.Seq[DecodeComponent])

  def accept[T](visitor: DecodeReferenceableVisitor[T]): T = visitor.visit(this)

  def languages: immutable.Seq[DecodeLanguage]

  def languages_=(languages: immutable.Seq[DecodeLanguage])

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

  def rootNamespace: DecodeNamespace = parent.map(_.rootNamespace).getOrElse(this)
}

trait DecodeNamespaceAware {
  def namespace: DecodeNamespace

  def namespace_=(namespace: DecodeNamespace)
}

trait DecodeOptionalNameAndOptionalInfoAware extends DecodeHasOptionInfo with DecodeOptionNamed

trait DecodeUnit extends DecodeNamed with DecodeHasOptionInfo with DecodeReferenceable with DecodeNamespaceAware {
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

trait DecodeNativeType extends DecodeType with DecodeNamed {
}

object DecodeNativeType {
  val MANGLED_TYPE_NAMES: Set[String] = immutable.HashSet[String](DecodeBerType.NAME, DecodeOrType.NAME, DecodeOptionalType.NAME)
}

trait BaseTyped {
  def baseType: DecodeMaybeProxy[DecodeType]
}

trait DecodeAliasType extends DecodeType with DecodeNamed with BaseTyped {
  def accept[T](visitor: DecodeTypeVisitor[T]): T = visitor.visit(this)
}

trait DecodeSubType extends DecodeType with BaseTyped {
  def accept[T](visitor: DecodeTypeVisitor[T]): T = visitor.visit(this)
}

trait DecodeEnumConstant extends DecodeHasOptionInfo {
  def name: DecodeName
  def value: String
}

trait DecodeEnumType extends DecodeType with BaseTyped {
  def constants: Set[DecodeEnumConstant]

  override def accept[T](visitor: DecodeTypeVisitor[T]): T = visitor.visit(this)
}

trait ArraySize {
  def min: Long
  def max: Long
}

trait DecodeArrayType extends DecodeType with BaseTyped {
  def size: ArraySize

  def isFixedSize: Boolean = {
    val thisSize: ArraySize = size
    val maxLength: Long = thisSize.max
    thisSize.min == maxLength && maxLength != 0
  }

  def accept[T](visitor: DecodeTypeVisitor[T]): T = visitor.visit(this)
}

trait DecodeTypeUnitApplication {
  def t: DecodeMaybeProxy[DecodeType]
  def unit: Option[DecodeMaybeProxy[DecodeUnit]]
}

trait DecodeStructField extends DecodeNamed with DecodeHasOptionInfo {
  def typeUnit: DecodeTypeUnitApplication
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
trait DecodeCommandParameter extends DecodeNamed with DecodeHasOptionInfo {
  def unit: Option[DecodeMaybeProxy[DecodeUnit]]
  def paramType: DecodeMaybeProxy[DecodeType]
}

trait DecodeCommand extends DecodeHasOptionInfo with DecodeNamed with DecodeHasOptionId {
  def returnType: Option[DecodeMaybeProxy[DecodeType]]
  def parameters: immutable.Seq[DecodeCommandParameter]
}

// TODO: replace with case classes?
trait DecodeMessageVisitor[T] {
  def visit(eventMessage: DecodeEventMessage): T
  def visit(statusMessage: DecodeStatusMessage): T
}

trait DecodeMessage extends DecodeHasOptionInfo with DecodeNamed with DecodeHasOptionId {
  def accept[T](visitor: DecodeMessageVisitor[T] ): T

  def parameters: Seq[DecodeMessageParameter]

  def component: DecodeComponent
}

trait DecodeStatusMessage extends DecodeMessage {
  def priority: Option[Int]

  def accept[T](visitor: DecodeMessageVisitor[T]): T = visitor.visit(this)
}

abstract class AbstractDecodeMessage(info: Option[String]) extends AbstractDecodeOptionalInfoAware(info)
with DecodeMessage

trait DecodeComponentRef {
  def component: DecodeMaybeProxy[DecodeComponent]

  def alias: Option[String]

  def aliasOrMangledName: String = alias.getOrElse(component.obj.name.asMangledString)
}

trait DecodeMessageParameterRef {
  def component: DecodeComponent
  def structField: Option[DecodeStructField]
  def subTokens: Seq[DecodeMessageParameterToken]
  def t: DecodeType
}

trait DecodeMessageParameter extends DecodeHasOptionInfo {
  def value: String

  def ref(component: DecodeComponent): DecodeMessageParameterRef =
    new DecodeMessageParameterRefWalker(component, None, tokens)

  private def tokens: Seq[DecodeMessageParameterToken] = DecodeParameterWalker(this).tokens
}

trait DecodeEventMessage extends DecodeMessage {
  def accept[T](visitor: DecodeMessageVisitor[T]): T = visitor.visit(this)
}

trait DecodeFqned extends DecodeNamed with DecodeNamespaceAware {
  def fqn: DecodeFqn = DecodeFqnImpl.newFromFqn(namespace.fqn, name)
}

trait DecodeComponent extends DecodeHasOptionInfo with DecodeFqned with DecodeReferenceable with DecodeHasOptionId {
  def messages: immutable.Seq[DecodeMessage]
  def messages_=(messages: immutable.Seq[DecodeMessage])
  def commands: immutable.Seq[DecodeCommand]
  def commands_=(commands: immutable.Seq[DecodeCommand])
  def baseType: Option[DecodeMaybeProxy[DecodeStructType]]
  def subComponents: immutable.Seq[DecodeComponentRef]
  override def accept[T](visitor: DecodeReferenceableVisitor[T]): T = visitor.visit(this)
}

trait DecodeDomainModelResolver {
  def resolve(decodeRegistry: DecodeRegistry): DecodeResolvingResult[DecodeReferenceable]
}

trait DecodeRegistry {
  def rootNamespaces: immutable.Seq[DecodeNamespace]

  def rootNamespaces_=(rootNamespaces: immutable.Seq[DecodeNamespace])

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

  // todo: refactoring
  def getNamespace(fqn: String): Option[DecodeNamespace] = {
    var currentNamespaces: Option[Seq[DecodeNamespace]] = Some(rootNamespaces)
    var currentNamespace: Option[DecodeNamespace] = None
    "\\.".r.split(fqn).foreach(nsName => {
      if (currentNamespaces.isEmpty)
      {
        return None
      }
      val decodeName = DecodeNameImpl.newFromMangledName(nsName)
      currentNamespace = currentNamespaces.get.find(_.name == decodeName)
      if (currentNamespace.isDefined)
      {
        currentNamespaces = Some(currentNamespace.get.subNamespaces)
      }
      else
      {
        currentNamespaces = None
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
