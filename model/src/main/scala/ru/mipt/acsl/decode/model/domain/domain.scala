package ru.mipt.acsl.decode.model.domain

import java.net.URI

import ru.mipt.acsl.decode.model.domain.impl.`type`._
import ru.mipt.acsl.decode.model.domain.impl.{MessageParameterRefWalker, DecodeNameImpl}

import scala.collection.immutable

object DecodeConstants {
  val SYSTEM_NAMESPACE_FQN: DecodeFqn = DecodeFqnImpl.newFromSource("decode")
}

package object aliases {
  type MessageParameterToken = Either[String, Int]
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

trait OptionNamed {
  def optionName: Option[DecodeName]
}

trait Named extends OptionNamed {
  def name: DecodeName
}

trait HasOptionInfo {
  def info: Option[String]
}

trait HasOptionId {
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

trait Referenceable extends OptionNamed {
  def accept[T](visitor: DecodeReferenceableVisitor[T]): T
}

trait Language extends Referenceable with NamespaceAware

trait Namespace extends Referenceable with Named {
  def asString: String

  def units: immutable.Seq[Measure]

  def units_=(units: immutable.Seq[Measure])

  def types: immutable.Seq[DecodeType]

  def types_=(types: immutable.Seq[DecodeType])

  def subNamespaces: immutable.Seq[Namespace]

  def subNamespaces_=(namespaces: immutable.Seq[Namespace])

  def parent: Option[Namespace]

  def parent_=(parent: Option[Namespace])

  def components: immutable.Seq[Component]

  def components_=(components: immutable.Seq[Component])

  def accept[T](visitor: DecodeReferenceableVisitor[T]): T = visitor.visit(this)

  def languages: immutable.Seq[Language]

  def languages_=(languages: immutable.Seq[Language])

  def fqn: DecodeFqn = {
    val parts: scala.collection.mutable.Buffer[DecodeName] = scala.collection.mutable.Buffer[DecodeName]()
    var currentNamespace: Namespace = this
    while (currentNamespace.parent.isDefined) {
      parts += currentNamespace.name
      currentNamespace = currentNamespace.parent.get
    }
    parts += currentNamespace.name
    DecodeFqnImpl(parts.reverse)
  }

  def rootNamespace: Namespace = parent.map(_.rootNamespace).getOrElse(this)
}

trait NamespaceAware {
  def namespace: Namespace

  def namespace_=(namespace: Namespace)
}

trait DecodeOptionalNameAndOptionalInfoAware extends HasOptionInfo with OptionNamed

trait Measure extends Named with HasOptionInfo with Referenceable with NamespaceAware {
  def display: Option[String]

  def accept[T] (visitor: DecodeReferenceableVisitor[T] ): T = visitor.visit(this)
}

trait DecodeReferenceableVisitor[T] {
  def visit(namespace: Namespace): T
  def visit(`type`: DecodeType): T
  def visit(component: Component): T
  def visit(measure: Measure): T
  def visit(language: Language): T
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

trait DecodeType extends Referenceable with DecodeOptionalNameAndOptionalInfoAware with NamespaceAware {
  def accept[T](visitor: DecodeTypeVisitor[T]): T

  def accept[T](visitor: DecodeReferenceableVisitor[T] ): T = visitor.visit(this)
}

trait PrimitiveType extends DecodeType {
  def bitLength: Long

  def kind: TypeKind.Value

  def accept[T](visitor: DecodeTypeVisitor[T]): T = visitor.visit(this)
}

trait NativeType extends DecodeType with Named {
}

object NativeType {
  val MANGLED_TYPE_NAMES: Set[String] = immutable.HashSet[String](BerType.NAME, OrType.NAME, OptionalType.NAME)
}

trait BaseTyped {
  def baseType: MaybeProxy[DecodeType]
}

trait AliasType extends DecodeType with Named with BaseTyped {
  def accept[T](visitor: DecodeTypeVisitor[T]): T = visitor.visit(this)
}

trait SubType extends DecodeType with BaseTyped {
  def accept[T](visitor: DecodeTypeVisitor[T]): T = visitor.visit(this)
}

trait DecodeEnumConstant extends HasOptionInfo {
  def name: DecodeName
  def value: String
}

trait EnumType extends DecodeType with BaseTyped {
  def constants: Set[DecodeEnumConstant]

  override def accept[T](visitor: DecodeTypeVisitor[T]): T = visitor.visit(this)
}

trait ArraySize {
  def min: Long
  def max: Long
}

trait ArrayType extends DecodeType with BaseTyped {
  def size: ArraySize

  def isFixedSize: Boolean = {
    val thisSize: ArraySize = size
    val maxLength: Long = thisSize.max
    thisSize.min == maxLength && maxLength != 0
  }

  def accept[T](visitor: DecodeTypeVisitor[T]): T = visitor.visit(this)
}

trait DecodeTypeUnitApplication {
  def t: MaybeProxy[DecodeType]
  def unit: Option[MaybeProxy[Measure]]
}

trait DecodeStructField extends Named with HasOptionInfo {
  def typeUnit: DecodeTypeUnitApplication
}

trait StructType extends DecodeType {
  def fields: Seq[DecodeStructField]

  override def accept[T](visitor: DecodeTypeVisitor[T]): T = visitor.visit(this)
}

trait GenericType extends DecodeType {
  def typeParameters: Seq[Option[DecodeName]]

  def accept[T](visitor: DecodeTypeVisitor[T]): T = visitor.visit(this)
}

trait GenericTypeSpecialized extends DecodeType {
  def genericType: MaybeProxy[GenericType]

  def genericTypeArguments: Seq[Option[MaybeProxy[DecodeType]]]

  def accept[T] (visitor: DecodeTypeVisitor[T]): T = visitor.visit(this)
}

trait DecodeTypeVisitor[T] {
  def visit(primitiveType: PrimitiveType): T
  def visit(nativeType: NativeType): T
  def visit(subType: SubType): T
  def visit(enumType: EnumType): T
  def visit(arrayType: ArrayType): T
  def visit(structType: StructType): T
  def visit(typeAlias: AliasType): T
  def visit(genericType: GenericType): T
  def visit(genericTypeSpecialized: GenericTypeSpecialized): T
}

// Components

trait Parameter extends Named with HasOptionInfo {
  def unit: Option[MaybeProxy[Measure]]
  def paramType: MaybeProxy[DecodeType]
}

trait DecodeCommand extends HasOptionInfo with Named with HasOptionId {
  def returnType: Option[MaybeProxy[DecodeType]]
  def parameters: immutable.Seq[Parameter]
}

// TODO: replace with case classes?
trait DecodeMessageVisitor[T] {
  def visit(eventMessage: EventMessage): T
  def visit(statusMessage: StatusMessage): T
}

trait Message extends HasOptionInfo with Named with HasOptionId {
  def accept[T](visitor: DecodeMessageVisitor[T] ): T

  def component: Component
}

trait StatusMessage extends Message {
  def priority: Option[Int]

  def parameters: Seq[MessageParameter]

  def accept[T](visitor: DecodeMessageVisitor[T]): T = visitor.visit(this)
}

abstract class AbstractMessage(info: Option[String]) extends AbstractDecodeOptionalInfoAware(info) with Message

trait DecodeComponentRef {
  def component: MaybeProxy[Component]

  def alias: Option[String]

  def aliasOrMangledName: String = alias.getOrElse(component.obj.name.asMangledString)
}

trait MessageParameterRef {
  def component: Component
  def structField: Option[DecodeStructField]
  def subTokens: Seq[MessageParameterToken]
  def t: DecodeType
}

trait MessageParameter extends HasOptionInfo {
  def value: String

  def ref(component: Component): MessageParameterRef =
    new MessageParameterRefWalker(component, None, tokens)

  private def tokens: Seq[MessageParameterToken] = ParameterWalker(this).tokens
}

trait EventMessage extends Message with BaseTyped {
  def fields: Seq[Either[MessageParameter, Parameter]]
  def accept[T](visitor: DecodeMessageVisitor[T]): T = visitor.visit(this)
}

trait Fqned extends Named with NamespaceAware {
  def fqn: DecodeFqn = DecodeFqnImpl.newFromFqn(namespace.fqn, name)
}

trait Component extends HasOptionInfo with Fqned with Referenceable with HasOptionId {
  def statusMessages: immutable.Seq[StatusMessage]
  def statusMessages_=(messages: immutable.Seq[StatusMessage])
  def eventMessages: immutable.Seq[EventMessage]
  def eventMessages_=(messages: immutable.Seq[EventMessage])
  def commands: immutable.Seq[DecodeCommand]
  def commands_=(commands: immutable.Seq[DecodeCommand])
  def baseType: Option[MaybeProxy[StructType]]
  def subComponents: immutable.Seq[DecodeComponentRef]
  override def accept[T](visitor: DecodeReferenceableVisitor[T]): T = visitor.visit(this)
}

trait DomainModelResolver {
  def resolve(registry: Registry): ResolvingResult[Referenceable]
}

trait Registry {
  def rootNamespaces: immutable.Seq[Namespace]

  def rootNamespaces_=(rootNamespaces: immutable.Seq[Namespace])

  def resolve[T <: Referenceable](uri: URI, cls: Class[T]): ResolvingResult[T]

  // TODO: refactoring
  def component(fqn: String): Option[Component] = {
    val dotPos = fqn.lastIndexOf('.')
    val namespaceOptional = namespace(fqn.substring(0, dotPos))
    if (namespaceOptional.isEmpty)
    {
      return None
    }
    val componentName = DecodeNameImpl.newFromMangledName(fqn.substring(dotPos + 1, fqn.length()))
    namespaceOptional.get.components.find(_.name == componentName)
  }

  // todo: refactoring
  def namespace(fqn: String): Option[Namespace] = {
    var currentNamespaces: Option[Seq[Namespace]] = Some(rootNamespaces)
    var currentNamespace: Option[Namespace] = None
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

  def eventMessage(fqn: String): Option[EventMessage] = {
    val dotPos = fqn.lastIndexOf('.')
    val decodeName = DecodeNameImpl.newFromMangledName(fqn.substring(dotPos + 1, fqn.length()))
    component(fqn.substring(0, dotPos)).map(_.eventMessages.find(_.name == decodeName).orNull)
  }

  def statusMessage(fqn: String): Option[StatusMessage] = {
    val dotPos = fqn.lastIndexOf('.')
    val decodeName = DecodeNameImpl.newFromMangledName(fqn.substring(dotPos + 1, fqn.length()))
    component(fqn.substring(0, dotPos)).map(_.statusMessages.find(_.name == decodeName).orNull)
  }

  def statusMessageOrFail(fqn: String): StatusMessage = {
    statusMessage(fqn).getOrElse(sys.error("assertion error"))
  }

  def eventMessageOrFail(fqn: String): EventMessage = {
    eventMessage(fqn).getOrElse(sys.error("assertion error"))
  }
}
