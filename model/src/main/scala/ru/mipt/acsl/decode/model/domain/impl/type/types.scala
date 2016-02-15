package ru.mipt.acsl.decode.model.domain.impl.`type`

import ru.mipt.acsl.decode.model.domain.aliases.MessageParameterToken
import ru.mipt.acsl.decode.model.domain._
import ru.mipt.acsl.decode.model.domain.impl.{ParameterParser, DecodeNameImpl}

import scala.collection.immutable

/**
  * @author Artem Shein
  */
case class DecodeFqnImpl(var parts: Seq[DecodeName]) extends DecodeFqn {
  def copyDropLast(): DecodeFqn = new DecodeFqnImpl(parts.dropRight(1))
}

object DecodeFqnImpl {
  def newFromFqn(fqn: DecodeFqn, last: DecodeName) = new DecodeFqnImpl(fqn.parts :+ last)
  def newFromSource(sourceText: String): DecodeFqn =
    new DecodeFqnImpl("\\.".r.split(sourceText).map(DecodeNameImpl.newFromSourceName))
}

abstract class AbstractDecodeOptionalInfoAware(val info: Option[String]) extends HasOptionInfo

abstract class AbstractDecodeOptionalNameAndOptionalInfoAware(val optionName: Option[DecodeName], info: Option[String])
  extends AbstractDecodeOptionalInfoAware(info) with DecodeOptionalNameAndOptionalInfoAware {
}

class AbstractDecodeNameAndOptionalInfoAware(private val _name: DecodeName, info: Option[String])
  extends AbstractDecodeOptionalInfoAware(info) with Named {
  override def name: DecodeName = _name
  override def optionName: Option[DecodeName] = Some(_name)
}

class AbstractNameNamespaceOptionalInfoAware(name: DecodeName, var namespace: Namespace, info: Option[String])
  extends AbstractDecodeNameAndOptionalInfoAware(name, info) with NamespaceAware

class MeasureImpl(name: DecodeName, var namespace: Namespace, var display: Option[String], info: Option[String])
  extends AbstractDecodeNameAndOptionalInfoAware(name, info) with Measure {
  override def accept[T] (visitor: DecodeReferenceableVisitor[T]): T = visitor.visit(this)
}

class NamespaceImpl(var name: DecodeName, var parent: Option[Namespace] = None,
                    var types: immutable.Seq[DecodeType] = immutable.Seq.empty,
                    var units: immutable.Seq[Measure] = immutable.Seq.empty,
                    var subNamespaces: immutable.Seq[Namespace] = immutable.Seq.empty,
                    var components: immutable.Seq[Component] = immutable.Seq.empty,
                    var languages: immutable.Seq[Language] = immutable.Seq.empty)
  extends Named with Namespace {
  override def optionName: Option[DecodeName] = Some(name)
  override def asString: String = name.asMangledString
}

object NamespaceImpl {
  def apply(name: DecodeName, parent: Option[Namespace]) = new NamespaceImpl(name, parent)
}

// Types

abstract class AbstractType(optionName: Option[DecodeName], var namespace: Namespace, info: Option[String])
  extends AbstractDecodeOptionalNameAndOptionalInfoAware(optionName, info) with DecodeType

abstract class AbstractTypeWithBaseType(name: Option[DecodeName], namespace: Namespace,
                                        info: Option[String], var baseType: MaybeProxy[DecodeType])
  extends AbstractType(name, namespace, info)

class PrimitiveTypeImpl(name: Option[DecodeName], namespace: Namespace, info: Option[String],
                        val kind: TypeKind.Value, val bitLength: Long = 0)
  extends AbstractType(name, namespace, info) with PrimitiveType

class AliasTypeImpl(val name: DecodeName, var namespace: Namespace, val baseType: MaybeProxy[DecodeType],
                    info: Option[String])
  extends AbstractDecodeOptionalInfoAware(info) with AliasType with BaseTyped {
  def optionName = Some(name)
}

class SubTypeImpl(optionName: Option[DecodeName], namespace: Namespace, info: Option[String],
                  val baseType: MaybeProxy[DecodeType])
  extends AbstractType(optionName, namespace, info) with SubType

class DecodeEnumConstantImpl(val name: DecodeName, val value: String, info: Option[String])
  extends AbstractDecodeOptionalInfoAware(info) with DecodeEnumConstant

class EnumTypeImpl(name: Option[DecodeName], namespace: Namespace, baseType: MaybeProxy[DecodeType],
                   info: Option[String], var constants: Set[DecodeEnumConstant])
  extends AbstractTypeWithBaseType(name, namespace, info, baseType) with EnumType {
}

class DecodeTypeUnitApplicationImpl(val t: MaybeProxy[DecodeType], val unit: Option[MaybeProxy[Measure]])
  extends DecodeTypeUnitApplication

class DecodeStructFieldImpl(val name: DecodeName, val typeUnit: DecodeTypeUnitApplication, info: Option[String])
  extends AbstractDecodeOptionalInfoAware(info) with DecodeStructField {
  override def optionName: Option[DecodeName] = Some(name)
  override def toString: String = s"${this.getClass.getSimpleName}{name = $name, typeUnit = $typeUnit, info = $info}"
}

class StructTypeImpl(name: Option[DecodeName], namespace: Namespace, info: Option[String], var fields: Seq[DecodeStructField])
  extends AbstractType(name, namespace, info) with StructType {
  override def toString: String =
    s"${this.getClass}{name = $name, namespace = $namespace, info = $info, fields = [${fields.map(_.toString).mkString(", ")}]"
}

case class ArraySizeImpl(min: Long = 0, max: Long = 0) extends ArraySize

class BerType(optionalName: Option[DecodeName], namespace: Namespace, info: Option[String])
  extends AbstractType(optionalName, namespace, info) with NativeType {
  override def name: DecodeName = optionalName.get
  def accept[T](visitor: DecodeTypeVisitor[T]): T = visitor.visit(this)
}

object BerType {
  val NAME: String = "ber"
  val MANGLED_NAME: DecodeName = DecodeNameImpl.newFromMangledName(NAME)
  def apply(namespace: Namespace, info: Option[String]) = new BerType(Some(MANGLED_NAME), namespace, info)
}

class ArrayTypeImpl(optionName: Option[DecodeName], ns: Namespace, info: Option[String],
                    val baseType: MaybeProxy[DecodeType], val size: ArraySize)
  extends AbstractType(optionName, ns, info) with ArrayType

class GenericTypeSpecializedImpl(optionName: Option[DecodeName], namespace: Namespace, info: Option[String],
                                 val genericType: MaybeProxy[GenericType], val genericTypeArguments: Seq[Option[MaybeProxy[DecodeType]]])
  extends AbstractType(optionName, namespace, info) with GenericTypeSpecialized

class OrType(name: Option[DecodeName], namespace: Namespace, info: Option[String])
  extends AbstractType(name, namespace, info) with GenericType {

  def typeParameters: Seq[Option[DecodeName]] = OrType.typeParameters
}

object OrType {
  val NAME = "or"
  val MANGLED_NAME: DecodeName = DecodeNameImpl.newFromMangledName(NAME)
  private val typeParameters: Seq[Option[DecodeName]] = Seq(Some(DecodeNameImpl.newFromMangledName("L")), Some(DecodeNameImpl.newFromMangledName("R")))
}

class OptionalType(name: Option[DecodeName], ns: Namespace, info: Option[String])
  extends AbstractType(name, ns, info) with GenericType {
  override def typeParameters: Seq[Option[DecodeName]] = OptionalType.typeParameters
}

object OptionalType {
  private val typeParameters: Seq[Option[DecodeName]] = Seq[Option[DecodeName]](Option.apply(DecodeNameImpl.newFromMangledName("T")))
  val NAME: String = "optional"
  val MANGLED_NAME: DecodeNameImpl = DecodeNameImpl.newFromMangledName(NAME)
}

// Components
class ParameterImpl(name: DecodeName, info: Option[String], val paramType: MaybeProxy[DecodeType],
                    val unit: Option[MaybeProxy[Measure]])
  extends AbstractDecodeNameAndOptionalInfoAware(name, info) with Parameter

class ComponentImpl(name: DecodeName, namespace: Namespace, var id: Option[Int],
                    var baseType: Option[MaybeProxy[StructType]], info: Option[String],
                    var subComponents: immutable.Seq[DecodeComponentRef],
                    var commands: immutable.Seq[DecodeCommand] = immutable.Seq.empty,
                    var messages: immutable.Seq[Message] = immutable.Seq.empty)
  extends AbstractNameNamespaceOptionalInfoAware(name, namespace, info) with Component

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