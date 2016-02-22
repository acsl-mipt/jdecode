package ru.mipt.acsl.decode.model.domain.impl.types

import ru.mipt.acsl.decode.model.domain.aliases.MessageParameterToken
import ru.mipt.acsl.decode.model.domain._
import ru.mipt.acsl.decode.model.domain.impl.{ElementName, ParameterParser}
import ru.mipt.acsl.decode.model.domain.proxy.aliases.ResolvingResult
import ru.mipt.acsl.decode.model.domain.proxy.{MaybeProxy}

import scala.collection.immutable
import scala.collection.mutable

/**
  * @author Artem Shein
  */
case class FqnImpl(var parts: Seq[ElementName]) extends Fqn {
  def copyDropLast(): Fqn = new FqnImpl(parts.dropRight(1))
}

object FqnImpl {
  def newFromFqn(fqn: Fqn, last: ElementName) = new FqnImpl(fqn.parts :+ last)
  def newFromSource(sourceText: String): Fqn =
    new FqnImpl("\\.".r.split(sourceText).map(ElementName.newFromSourceName))
}

abstract class AbstractDecodeOptionalInfoAware(val info: Option[String]) extends HasOptionInfo

abstract class AbstractOptionNameAndOptionInfoAware(val optionName: Option[ElementName], info: Option[String])
  extends AbstractDecodeOptionalInfoAware(info) with OptionNameAndOptionInfoAware {
}

class AbstractDecodeNameAndOptionalInfoAware(private val _name: ElementName, info: Option[String])
  extends AbstractDecodeOptionalInfoAware(info) with HasName {
  override def name: ElementName = _name
  override def optionName: Option[ElementName] = Some(_name)
}

class AbstractNameNamespaceOptionalInfoAware(name: ElementName, var namespace: Namespace, info: Option[String])
  extends AbstractDecodeNameAndOptionalInfoAware(name, info) with NamespaceAware

class MeasureImpl(name: ElementName, var namespace: Namespace, var display: Option[String], info: Option[String])
  extends AbstractDecodeNameAndOptionalInfoAware(name, info) with Measure {
}

class NamespaceImpl(var name: ElementName, var parent: Option[Namespace] = None,
                    var types: immutable.Seq[DecodeType] = immutable.Seq.empty,
                    var units: immutable.Seq[Measure] = immutable.Seq.empty,
                    var subNamespaces: immutable.Seq[Namespace] = immutable.Seq.empty,
                    var components: immutable.Seq[Component] = immutable.Seq.empty,
                    var languages: immutable.Seq[Language] = immutable.Seq.empty)
  extends HasName with Namespace {
  override def optionName: Option[ElementName] = Some(name)
  override def asString: String = name.asMangledString

  override def resolve(registry: Registry): ResolvingResult = {
    val result = new mutable.ArrayBuffer[ResolvingResult]()
    result ++= types.map(_.resolve(registry))
    result ++= subNamespaces.map(_.resolve(registry))
    result ++= components.map(_.resolve(registry))
    result.flatten
  }
}

object NamespaceImpl {
  def apply(name: ElementName, parent: Option[Namespace]) = new NamespaceImpl(name, parent)
}

// Types

abstract class AbstractType(optionName: Option[ElementName], var namespace: Namespace, info: Option[String])
  extends AbstractOptionNameAndOptionInfoAware(optionName, info) with DecodeType

abstract class AbstractTypeWithBaseType(name: Option[ElementName], namespace: Namespace,
                                        info: Option[String], var baseType: MaybeProxy[DecodeType])
  extends AbstractType(name, namespace, info)

class PrimitiveTypeImpl(name: Option[ElementName], namespace: Namespace, info: Option[String],
                        val kind: TypeKind.Value, val bitLength: Long = 0)
  extends AbstractType(name, namespace, info) with PrimitiveType

class AliasTypeImpl(val name: ElementName, var namespace: Namespace, val baseType: MaybeProxy[DecodeType],
                    info: Option[String])
  extends AbstractDecodeOptionalInfoAware(info) with AliasType with HasBaseType {
  def optionName = Some(name)
}

class SubTypeImpl(optionName: Option[ElementName], namespace: Namespace, info: Option[String],
                  val baseType: MaybeProxy[DecodeType])
  extends AbstractType(optionName, namespace, info) with SubType

class EnumConstantImpl(val name: ElementName, val value: String, info: Option[String])
  extends AbstractDecodeOptionalInfoAware(info) with EnumConstant

class EnumTypeImpl(name: Option[ElementName], namespace: Namespace, baseType: MaybeProxy[DecodeType],
                   info: Option[String], var constants: Set[EnumConstant])
  extends AbstractTypeWithBaseType(name, namespace, info, baseType) with EnumType {
}

class TypeUnitImpl(val t: MaybeProxy[DecodeType], val unit: Option[MaybeProxy[Measure]])
  extends TypeUnit

class StructFieldImpl(val name: ElementName, val typeUnit: TypeUnit, info: Option[String])
  extends AbstractDecodeOptionalInfoAware(info) with StructField {
  override def optionName: Option[ElementName] = Some(name)
  override def toString: String = s"${this.getClass.getSimpleName}{name = $name, typeUnit = $typeUnit, info = $info}"
}

class StructTypeImpl(name: Option[ElementName], namespace: Namespace, info: Option[String], var fields: Seq[StructField])
  extends AbstractType(name, namespace, info) with StructType {
  override def toString: String =
    s"${this.getClass}{name = $name, namespace = $namespace, info = $info, fields = [${fields.map(_.toString).mkString(", ")}]"
}

case class ArraySizeImpl(min: Long = 0, max: Long = 0) extends ArraySize {
  require(min >= 0)
  require(max >= 0)
}

class BerType(optionalName: Option[ElementName], namespace: Namespace, info: Option[String])
  extends AbstractType(optionalName, namespace, info) with NativeType {
  override def name: ElementName = optionalName.get
}

object BerType {
  val NAME: String = "ber"
  val MANGLED_NAME: ElementName = ElementName.newFromMangledName(NAME)
  def apply(namespace: Namespace, info: Option[String]) = new BerType(Some(MANGLED_NAME), namespace, info)
}

class ArrayTypeImpl(optionName: Option[ElementName], ns: Namespace, info: Option[String],
                    val baseType: MaybeProxy[DecodeType], val size: ArraySize)
  extends AbstractType(optionName, ns, info) with ArrayType

class GenericTypeSpecializedImpl(optionName: Option[ElementName], namespace: Namespace, info: Option[String],
                                 val genericType: MaybeProxy[GenericType], val genericTypeArguments: Seq[Option[MaybeProxy[DecodeType]]])
  extends AbstractType(optionName, namespace, info) with GenericTypeSpecialized

class OrType(name: Option[ElementName], namespace: Namespace, info: Option[String])
  extends AbstractType(name, namespace, info) with GenericType {

  def typeParameters: Seq[Option[ElementName]] = OrType.typeParameters
}

object OrType {
  val NAME = "or"
  val MANGLED_NAME: ElementName = ElementName.newFromMangledName(NAME)
  private val typeParameters: Seq[Option[ElementName]] =
    Seq(Some(ElementName.newFromMangledName("L")), Some(ElementName.newFromMangledName("R")))
}

class OptionalType(name: Option[ElementName], ns: Namespace, info: Option[String])
  extends AbstractType(name, ns, info) with GenericType {
  override def typeParameters: Seq[Option[ElementName]] = OptionalType.typeParameters
}

object OptionalType {
  private val typeParameters: Seq[Option[ElementName]] =
    Seq[Option[ElementName]](Option.apply(ElementName.newFromMangledName("T")))
  val NAME: String = "optional"
  val MANGLED_NAME: ElementName = ElementName.newFromMangledName(NAME)
}

// Components
class ParameterImpl(name: ElementName, info: Option[String], val paramType: MaybeProxy[DecodeType],
                    val unit: Option[MaybeProxy[Measure]])
  extends AbstractDecodeNameAndOptionalInfoAware(name, info) with Parameter

class ComponentImpl(name: ElementName, namespace: Namespace, var id: Option[Int],
                    var baseType: Option[MaybeProxy[StructType]], info: Option[String],
                    var subComponents: immutable.Seq[DecodeComponentRef],
                    var commands: immutable.Seq[Command] = immutable.Seq.empty,
                    var eventMessages: immutable.Seq[EventMessage] = immutable.Seq.empty,
                    var statusMessages: immutable.Seq[StatusMessage] = immutable.Seq.empty)
  extends AbstractNameNamespaceOptionalInfoAware(name, namespace, info) with Component {
  override def resolve(registry: Registry): ResolvingResult = {
    val resultList = mutable.Buffer.empty[ResolvingResult]
    baseType.map { t =>
      resultList += t.resolve(registry)
      if (t.isResolved)
        resultList += t.obj.resolve(registry)
    }
    commands.foreach { cmd =>
      cmd.returnType.foreach(rt => resultList += rt.resolve(registry))
      cmd.parameters.foreach { arg =>
        resultList += arg.paramType.resolve(registry)
        arg.unit.map(u => resultList += u.resolve(registry))
      }
    }
    eventMessages.foreach { e =>
      resultList += e.baseType.resolve(registry)
      e.fields.foreach {
        case Right(p) => resultList += p.paramType.resolve(registry)
        case _ =>
      }
    }
    subComponents.foreach { scr =>
      val sc = scr.component
      resultList += sc.resolve(registry)
      if (sc.isResolved)
        resultList += sc.obj.resolve(registry)
    }
    resultList.flatten
  }
}

case class ParameterWalker(parameter: MessageParameter)
{
  private var currentIndex = 0
  private val result = new ParameterParser(parameter.value).Parameter.run()
  if (result.isFailure)
    sys.error("parsing fails")
  val tokens = result.get

  def hasNext: Boolean = currentIndex < tokens.size

  def next: MessageParameterToken = { currentIndex += 1; tokens(currentIndex - 1) }
}