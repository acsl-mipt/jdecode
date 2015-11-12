package ru.mipt.acsl.decode.model.domain.impl.`type`

import ru.mipt.acsl.decode.model.domain._
import ru.mipt.acsl.decode.model.domain.impl.{ParameterParser, DecodeNameImpl}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/**
  * @author Artem Shein
  */
case class DecodeFqnImpl(parts: Seq[DecodeName]) extends DecodeFqn {

  def copyDropLast(): DecodeFqn = DecodeFqnImpl(parts.dropRight(1))
}

object DecodeFqnImpl {
  def newFromSource(sourceText: String): DecodeFqn = new DecodeFqnImpl(".".r.split(sourceText).map(DecodeNameImpl.newFromSourceName))
}

abstract class AbstractDecodeOptionalInfoAware(val info: Option[String]) extends DecodeOptionalInfoAware

abstract class AbstractDecodeOptionalNameAndOptionalInfoAware(val optionalName: Option[DecodeName], info: Option[String])
  extends AbstractDecodeOptionalInfoAware(info) with DecodeOptionalNameAndOptionalInfoAware {
}

class AbstractDecodeNameAndOptionalInfoAware(val _name: DecodeName, info: Option[String])
  extends AbstractDecodeOptionalInfoAware(info) with DecodeNameAware {
  override def name: DecodeName = _name
  override def optionalName: Option[DecodeName] = Some(_name)
}

class AbstractDecodeNameNamespaceOptionalInfoAware(name: DecodeName, var namespace: DecodeNamespace, info: Option[String])
  extends AbstractDecodeNameAndOptionalInfoAware(name, info) with DecodeNamespaceAware

class DecodeUnitImpl(name: DecodeName, var namespace: DecodeNamespace, var display: Option[String], info: Option[String])
  extends AbstractDecodeNameAndOptionalInfoAware(name, info) with DecodeUnit {
  override def accept[T] (visitor: DecodeReferenceableVisitor[T]): T = visitor.visit(this)
}

class DecodeNamespaceImpl(var name: DecodeName, var parent: Option[DecodeNamespace] = None,
                          var types: mutable.Buffer[DecodeType] = mutable.Buffer(), var units: mutable.Buffer[DecodeUnit] = mutable.Buffer(),
                          var subNamespaces: mutable.Buffer[DecodeNamespace] = mutable.Buffer(), var components: mutable.Buffer[DecodeComponent] = mutable.Buffer(),
                          var languages: mutable.Buffer[DecodeLanguage] = mutable.Buffer())
  extends DecodeNameAware with DecodeNamespace {
  override def optionalName: Option[DecodeName] = Some(name)
  override def asString: String = name.asString()
}

// TODO: remove me
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
  def optionalName = Some(name)
}

class DecodeSubTypeImpl(optionName: Option[DecodeName], namespace: DecodeNamespace, info: Option[String],
                        val baseType: DecodeMaybeProxy[DecodeType])
  extends AbstractDecodeType(optionName, namespace, info) with DecodeSubType

class DecodeEnumTypeImpl(name: Option[DecodeName], namespace: DecodeNamespace, baseType: DecodeMaybeProxy[DecodeType],
                              info: Option[String], var constants: mutable.Set[DecodeEnumConstant])
  extends AbstractDecodeTypeWithBaseType(name, namespace, info, baseType) with DecodeEnumType {
}

class DecodeStructTypeImpl(name: Option[DecodeName], namespace: DecodeNamespace, info: Option[String], var fields: Seq[DecodeStructField])
  extends AbstractDecodeType(name, namespace, info) with DecodeStructType

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
  val MANGLED_NAME: DecodeName = DecodeNameImpl.newFromMangledName ("or")
  private val typeParameters: Seq[Option[DecodeName]] = Seq(Some(DecodeNameImpl.newFromMangledName ("L")), Some(DecodeNameImpl.newFromMangledName("R")))
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
class DecodeCommandArgumentImpl(name: DecodeName, info: Option[String], val argType: DecodeMaybeProxy[DecodeType],
                                val unit: Option[DecodeMaybeProxy[DecodeUnit]])
  extends AbstractDecodeNameAndOptionalInfoAware(name, info) with DecodeCommandArgument

class DecodeComponentImpl(name: DecodeName, namespace: DecodeNamespace, var id: Option[Int],
                            var baseType: Option[DecodeMaybeProxy[DecodeStructType]], info: Option[String],
                            var subComponents: Seq[DecodeComponentRef], var commands: Seq[DecodeCommand],
                            var messages: Seq[DecodeMessage])
  extends AbstractDecodeNameNamespaceOptionalInfoAware(name, namespace, info) with DecodeComponent

class DecodeParameterWalker(var parameter: DecodeMessageParameter)
{
  private var currentIndex = 0
  private val tokens = ArrayBuffer[Either[String, Int]]()

  type Val = Seq[Either[String, Int]]

  val result = new ParameterParser(parameter.value).Parameter.run()
  if (result.isFailure)
    sys.error("parsing fails")
  tokens ++= result.get

  def hasNext: Boolean = currentIndex < tokens.size

  def next: Either[String, Int] = { currentIndex += 1; tokens(currentIndex - 1) }
}