package ru.mipt.acsl.decode.model.domain

import ru.mipt.acsl.decode.model.domain.aliases.MessageParameterToken
import ru.mipt.acsl.decode.model.domain.impl.types._
import ru.mipt.acsl.decode.model.domain.impl.{ElementName, MessageParameterRefWalker}
import ru.mipt.acsl.decode.model.domain.proxy.aliases.ResolvingResult
import ru.mipt.acsl.decode.model.domain.proxy.{MaybeProxy, ProxyPath}

import scala.collection.{mutable, immutable}
import scala.reflect.ClassTag

object DecodeConstants {
  val SYSTEM_NAMESPACE_FQN: Fqn = FqnImpl.newFromSource("decode")
}

package object aliases {
  type MessageParameterToken = Either[String, Int]
}

trait Resolvable {
  def resolve(registry: Registry): ResolvingResult
}

trait ElementName {
  def asMangledString: String
}

trait HasOptionName {
  def optionName: Option[ElementName]
}

trait HasName extends HasOptionName {
  def name: ElementName
}

trait HasOptionInfo {
  def info: Option[String]
}

trait HasOptionId {
  def id: Option[Int]
}

trait Fqn {
  def parts: Seq[ElementName]

  def asMangledString: String = parts.map(_.asMangledString).mkString(".")

  def last: ElementName = parts.last

  def copyDropLast(): Fqn

  def size: Int = parts.size

  def isEmpty: Boolean = parts.isEmpty
}

trait Referenceable extends HasOptionName

trait Language extends Referenceable with NamespaceAware

trait Namespace extends Referenceable with HasName with Resolvable {
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

  def languages: immutable.Seq[Language]

  def languages_=(languages: immutable.Seq[Language])

  def fqn: Fqn = {
    val parts: scala.collection.mutable.Buffer[ElementName] = scala.collection.mutable.Buffer[ElementName]()
    var currentNamespace: Namespace = this
    while (currentNamespace.parent.isDefined) {
      parts += currentNamespace.name
      currentNamespace = currentNamespace.parent.get
    }
    parts += currentNamespace.name
    FqnImpl(parts.reverse)
  }

  def rootNamespace: Namespace = parent.map(_.rootNamespace).getOrElse(this)
}

trait NamespaceAware {
  def namespace: Namespace

  def namespace_=(namespace: Namespace)
}

trait OptionNameAndOptionInfoAware extends HasOptionInfo with HasOptionName

trait Measure extends HasName with HasOptionInfo with Referenceable with NamespaceAware {
  def display: Option[String]
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

trait DecodeType extends Referenceable with OptionNameAndOptionInfoAware with NamespaceAware with Resolvable {
  override def resolve(registry: Registry): ResolvingResult = {
    val resolvingResultList = mutable.Buffer.empty[ResolvingResult]
    this match {
      case t: HasBaseType =>
        resolvingResultList += t.baseType.resolve(registry)
      case t: StructType =>
        t.fields.foreach { f =>
          val typeUnit = f.typeUnit
          resolvingResultList += typeUnit.t.resolve(registry)
          if (typeUnit.t.isResolved)
            resolvingResultList += typeUnit.t.obj.resolve(registry)
          for (unit <- typeUnit.unit)
            resolvingResultList += unit.resolve(registry)
        }
      case t: GenericTypeSpecialized =>
        resolvingResultList += t.genericType.resolve(registry)
        t.genericTypeArguments.foreach(_.foreach(gta =>
          resolvingResultList += gta.resolve(registry)))
      case _ =>
    }
    resolvingResultList.flatten
  }
}

trait PrimitiveType extends DecodeType {
  def bitLength: Long

  def kind: TypeKind.Value
}

trait NativeType extends DecodeType with HasName {
}

object NativeType {
  val MANGLED_TYPE_NAMES: Set[String] = immutable.HashSet[String](BerType.NAME, OrType.NAME, OptionalType.NAME)
}

trait HasBaseType {
  def baseType: MaybeProxy[DecodeType]
}

trait AliasType extends DecodeType with HasName with HasBaseType

trait SubType extends DecodeType with HasBaseType

trait EnumConstant extends HasOptionInfo {
  def name: ElementName
  def value: String
}

trait EnumType extends DecodeType with HasBaseType {
  def constants: Set[EnumConstant]
}

trait ArraySize {
  def min: Long
  def max: Long
}

trait ArrayType extends DecodeType with HasBaseType {
  def size: ArraySize

  def isFixedSize: Boolean = {
    val thisSize: ArraySize = size
    val maxLength: Long = thisSize.max
    thisSize.min == maxLength && maxLength != 0
  }
}

trait TypeUnit {
  def t: MaybeProxy[DecodeType]
  def unit: Option[MaybeProxy[Measure]]
}

trait StructField extends HasName with HasOptionInfo {
  def typeUnit: TypeUnit
}

trait StructType extends DecodeType {
  def fields: Seq[StructField]
}

trait GenericType extends DecodeType {
  def typeParameters: Seq[Option[ElementName]]
}

trait GenericTypeSpecialized extends DecodeType {
  def genericType: MaybeProxy[GenericType]

  def genericTypeArguments: Seq[Option[MaybeProxy[DecodeType]]]
}

// Components

trait Parameter extends HasName with HasOptionInfo {
  def unit: Option[MaybeProxy[Measure]]
  def paramType: MaybeProxy[DecodeType]
}

trait Command extends HasOptionInfo with HasName with HasOptionId {
  def returnType: Option[MaybeProxy[DecodeType]]
  def parameters: immutable.Seq[Parameter]
}

trait TmMessage extends HasOptionInfo with HasName with HasOptionId {
  def component: Component
}

trait StatusMessage extends TmMessage {
  def priority: Option[Int]

  def parameters: Seq[MessageParameter]
}

abstract class AbstractMessage(info: Option[String]) extends AbstractDecodeOptionalInfoAware(info) with TmMessage

trait DecodeComponentRef {
  def component: MaybeProxy[Component]

  def alias: Option[String]

  def aliasOrMangledName: String = alias.getOrElse(component.obj.name.asMangledString)
}

trait MessageParameterRef {
  def component: Component
  def structField: Option[StructField]
  def subTokens: Seq[MessageParameterToken]
  def t: DecodeType
}

trait MessageParameter extends HasOptionInfo {
  def value: String

  def ref(component: Component): MessageParameterRef =
    new MessageParameterRefWalker(component, None, tokens)

  private def tokens: Seq[MessageParameterToken] = ParameterWalker(this).tokens
}

trait EventMessage extends TmMessage with HasBaseType {
  def fields: Seq[Either[MessageParameter, Parameter]]
}

trait Fqned extends HasName with NamespaceAware {
  def fqn: Fqn = FqnImpl.newFromFqn(namespace.fqn, name)
}

trait Component extends HasOptionInfo with Fqned with Referenceable with HasOptionId with Resolvable {
  def statusMessages: immutable.Seq[StatusMessage]
  def statusMessages_=(messages: immutable.Seq[StatusMessage]): Unit
  def eventMessages: immutable.Seq[EventMessage]
  def eventMessages_=(messages: immutable.Seq[EventMessage]): Unit
  def commands: immutable.Seq[Command]
  def commands_=(commands: immutable.Seq[Command]): Unit
  def baseType: Option[MaybeProxy[StructType]]
  def baseType_=(maybeProxy: Option[MaybeProxy[StructType]]): Unit
  def subComponents: immutable.Seq[DecodeComponentRef]
}

trait DomainModelResolver {
  def resolve(registry: Registry): ResolvingResult
}

trait Registry extends Referenceable with HasName with Resolvable {
  def rootNamespaces: immutable.Seq[Namespace]

  def rootNamespaces_=(rootNamespaces: immutable.Seq[Namespace])

  def resolve(): ResolvingResult

  def resolveElement[T <: Referenceable](path: ProxyPath)(implicit ct: ClassTag[T]): (Option[T], ResolvingResult)

  // TODO: refactoring
  def component(fqn: String): Option[Component] = {
    val dotPos = fqn.lastIndexOf('.')
    val namespaceOptional = namespace(fqn.substring(0, dotPos))
    if (namespaceOptional.isEmpty)
    {
      return None
    }
    val componentName = ElementName.newFromMangledName(fqn.substring(dotPos + 1, fqn.length()))
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
      val decodeName = ElementName.newFromMangledName(nsName)
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
    val decodeName = ElementName.newFromMangledName(fqn.substring(dotPos + 1, fqn.length()))
    component(fqn.substring(0, dotPos)).map(_.eventMessages.find(_.name == decodeName).orNull)
  }

  def statusMessage(fqn: String): Option[StatusMessage] = {
    val dotPos = fqn.lastIndexOf('.')
    val decodeName = ElementName.newFromMangledName(fqn.substring(dotPos + 1, fqn.length()))
    component(fqn.substring(0, dotPos)).map(_.statusMessages.find(_.name == decodeName).orNull)
  }

  def statusMessageOrFail(fqn: String): StatusMessage = {
    statusMessage(fqn).getOrElse(sys.error("assertion error"))
  }

  def eventMessageOrFail(fqn: String): EventMessage = {
    eventMessage(fqn).getOrElse(sys.error("assertion error"))
  }
}
