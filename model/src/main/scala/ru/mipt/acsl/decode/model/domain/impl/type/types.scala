package ru.mipt.acsl.decode.model.domain.impl.`type`

import ru.mipt.acsl.decode.model.domain.aliases.DecodeMessageParameterToken
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

abstract class AbstractDecodeOptionalInfoAware(val info: Option[String]) extends DecodeHasOptionInfo

abstract class AbstractDecodeOptionalNameAndOptionalInfoAware(val optionName: Option[DecodeName], info: Option[String])
  extends AbstractDecodeOptionalInfoAware(info) with DecodeOptionalNameAndOptionalInfoAware {
}

class AbstractDecodeNameAndOptionalInfoAware(private val _name: DecodeName, info: Option[String])
  extends AbstractDecodeOptionalInfoAware(info) with DecodeNamed {
  override def name: DecodeName = _name
  override def optionName: Option[DecodeName] = Some(_name)
}

class AbstractDecodeNameNamespaceOptionalInfoAware(name: DecodeName, var namespace: DecodeNamespace, info: Option[String])
  extends AbstractDecodeNameAndOptionalInfoAware(name, info) with DecodeNamespaceAware

class DecodeUnitImpl(name: DecodeName, var namespace: DecodeNamespace, var display: Option[String], info: Option[String])
  extends AbstractDecodeNameAndOptionalInfoAware(name, info) with DecodeUnit {
  override def accept[T] (visitor: DecodeReferenceableVisitor[T]): T = visitor.visit(this)
}

class DecodeNamespaceImpl(var name: DecodeName, var parent: Option[DecodeNamespace] = None,
                          var types: immutable.Seq[DecodeType] = immutable.Seq.empty,
                          var units: immutable.Seq[DecodeUnit] = immutable.Seq.empty,
                          var subNamespaces: immutable.Seq[DecodeNamespace] = immutable.Seq.empty,
                          var components: immutable.Seq[DecodeComponent] = immutable.Seq.empty,
                          var languages: immutable.Seq[DecodeLanguage] = immutable.Seq.empty)
  extends DecodeNamed with DecodeNamespace {
  override def optionName: Option[DecodeName] = Some(name)
  override def asString: String = name.asMangledString
}

object DecodeNamespaceImpl {
  def apply(name: DecodeName, parent: Option[DecodeNamespace]) = new DecodeNamespaceImpl(name, parent)
}

// Types

abstract class AbstractDecodeType(optionName: Option[DecodeName], var namespace: DecodeNamespace, info: Option[String])
  extends AbstractDecodeOptionalNameAndOptionalInfoAware(optionName, info) with DecodeType

abstract class AbstractDecodeTypeWithBaseType(name: Option[DecodeName], namespace: DecodeNamespace,
                                              info: Option[String], var baseType: DecodeMaybeProxy[DecodeType])
  extends AbstractDecodeType(name, namespace, info)

class DecodePrimitiveTypeImpl(name: Option[DecodeName], namespace: DecodeNamespace, info: Option[String],
                              val kind: TypeKind.Value, val bitLength: Long = 0)
  extends AbstractDecodeType(name, namespace, info) with DecodePrimitiveType

class DecodeAliasTypeImpl(val name: DecodeName, var namespace: DecodeNamespace, val baseType: DecodeMaybeProxy[DecodeType],
                          info: Option[String])
  extends AbstractDecodeOptionalInfoAware(info) with DecodeAliasType with BaseTyped {
  def optionName = Some(name)
}

class DecodeSubTypeImpl(optionName: Option[DecodeName], namespace: DecodeNamespace, info: Option[String],
                        val baseType: DecodeMaybeProxy[DecodeType])
  extends AbstractDecodeType(optionName, namespace, info) with DecodeSubType

class DecodeEnumConstantImpl(val name: DecodeName, val value: String, info: Option[String])
  extends AbstractDecodeOptionalInfoAware(info) with DecodeEnumConstant

class DecodeEnumTypeImpl(name: Option[DecodeName], namespace: DecodeNamespace, baseType: DecodeMaybeProxy[DecodeType],
                              info: Option[String], var constants: Set[DecodeEnumConstant])
  extends AbstractDecodeTypeWithBaseType(name, namespace, info, baseType) with DecodeEnumType {
}

class DecodeTypeUnitApplicationImpl(val t: DecodeMaybeProxy[DecodeType], val unit: Option[DecodeMaybeProxy[DecodeUnit]])
  extends DecodeTypeUnitApplication

class DecodeStructFieldImpl(val name: DecodeName, val typeUnit: DecodeTypeUnitApplication, info: Option[String])
  extends AbstractDecodeOptionalInfoAware(info) with DecodeStructField {
  override def optionName: Option[DecodeName] = Some(name)
  override def toString: String = s"${this.getClass.getSimpleName}{name = $name, typeUnit = $typeUnit, info = $info}"
}

class DecodeStructTypeImpl(name: Option[DecodeName], namespace: DecodeNamespace, info: Option[String], var fields: Seq[DecodeStructField])
  extends AbstractDecodeType(name, namespace, info) with DecodeStructType {
  override def toString: String =
    s"${this.getClass}{name = $name, namespace = $namespace, info = $info, fields = [${fields.map(_.toString).mkString(", ")}]"
}

case class ArraySizeImpl(minLength: Long = 0, maxLength: Long = 0) extends ArraySize

class DecodeBerType(optionalName: Option[DecodeName], namespace: DecodeNamespace, info: Option[String])
  extends AbstractDecodeType(optionalName, namespace, info) with DecodeNativeType {
  override def name: DecodeName = optionalName.get
  def accept[T](visitor: DecodeTypeVisitor[T]): T = visitor.visit(this)
}

object DecodeBerType {
  val NAME: String = "ber"
  val MANGLED_NAME: DecodeName = DecodeNameImpl.newFromMangledName(NAME)
  def apply(namespace: DecodeNamespace, info: Option[String]) = new DecodeBerType(Some(MANGLED_NAME), namespace, info)
}

class DecodeArrayTypeImpl(optionName: Option[DecodeName], ns: DecodeNamespace, info: Option[String],
                          val baseType: DecodeMaybeProxy[DecodeType], val size: ArraySize)
  extends AbstractDecodeType(optionName, ns, info) with DecodeArrayType

class DecodeGenericTypeSpecializedImpl(optionName: Option[DecodeName], namespace: DecodeNamespace, info: Option[String],
  val genericType: DecodeMaybeProxy[DecodeGenericType], val genericTypeArguments: Seq[Option[DecodeMaybeProxy[DecodeType]]])
  extends AbstractDecodeType(optionName, namespace, info) with DecodeGenericTypeSpecialized

class DecodeOrType(name: Option[DecodeName], namespace: DecodeNamespace, info: Option[String])
  extends AbstractDecodeType(name, namespace, info) with DecodeGenericType {

  def typeParameters: Seq[Option[DecodeName]] = DecodeOrType.typeParameters
}

object DecodeOrType {
  val NAME = "or"
  val MANGLED_NAME: DecodeName = DecodeNameImpl.newFromMangledName(NAME)
  private val typeParameters: Seq[Option[DecodeName]] = Seq(Some(DecodeNameImpl.newFromMangledName("L")), Some(DecodeNameImpl.newFromMangledName("R")))
}

class DecodeOptionalType(name: Option[DecodeName], ns: DecodeNamespace, info: Option[String])
  extends AbstractDecodeType(name, ns, info) with DecodeGenericType {
  override def typeParameters: Seq[Option[DecodeName]] = DecodeOptionalType.typeParameters
}

object DecodeOptionalType {
  private val typeParameters: Seq[Option[DecodeName]] = Seq[Option[DecodeName]](Option.apply(DecodeNameImpl.newFromMangledName("T")))
  val NAME: String = "optional"
  val MANGLED_NAME: DecodeNameImpl = DecodeNameImpl.newFromMangledName(NAME)
}

// Components
class DecodeCommandParameterImpl(name: DecodeName, info: Option[String], val paramType: DecodeMaybeProxy[DecodeType],
                                 val unit: Option[DecodeMaybeProxy[DecodeUnit]])
  extends AbstractDecodeNameAndOptionalInfoAware(name, info) with DecodeCommandParameter

class DecodeComponentImpl(name: DecodeName, namespace: DecodeNamespace, var id: Option[Int],
                          var baseType: Option[DecodeMaybeProxy[DecodeStructType]], info: Option[String],
                          var subComponents: immutable.Seq[DecodeComponentRef],
                          var commands: immutable.Seq[DecodeCommand] = immutable.Seq.empty,
                          var messages: immutable.Seq[DecodeMessage] = immutable.Seq.empty)
  extends AbstractDecodeNameNamespaceOptionalInfoAware(name, namespace, info) with DecodeComponent

case class DecodeParameterWalker(parameter: DecodeMessageParameter)
{
  private var currentIndex = 0
  private val result = new ParameterParser(parameter.value).Parameter.run()
  if (result.isFailure)
    sys.error("parsing fails")
  val tokens = result.get

  def hasNext: Boolean = currentIndex < tokens.size

  def next: DecodeMessageParameterToken = { currentIndex += 1; tokens(currentIndex - 1) }
}