package ru.mipt.acsl.decode.model.domain.impl.`type`

import ru.mipt.acsl.decode.model.domain.TypeKind.TypeKind
import ru.mipt.acsl.decode.model.domain._
import ru.mipt.acsl.decode.model.domain.impl.{AbstractDecodeNameNamespaceOptionalInfoAware, DecodeNameImpl}
import ru.mipt.acsl.decode.model.domain.message.DecodeMessage
import ru.mipt.acsl.decode.model.domain.proxy.DecodeMaybeProxy

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
                          var types: Seq[DecodeType] = Seq(), var units: Seq[DecodeUnit] = Seq(),
                          var subNamespaces: Seq[DecodeNamespace] = Seq(), var components: Seq[DecodeComponent] = Seq(),
                          var languages: Seq[DecodeLanguage] = Seq())
  extends DecodeNameAware with DecodeNamespace {
  override def optionalName: Option[DecodeName] = Some(name)
  override def asString: String = name.asString()
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

class DecodeEnumTypeImpl(name: Option[DecodeName], namespace: DecodeNamespace, baseType: DecodeMaybeProxy[DecodeType],
                              info: Option[String], var constants: Set[DecodeEnumConstant])
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

class DecodeOrType(name: Option[DecodeName], namespace: DecodeNamespace, info: Option[String])
  extends AbstractDecodeType(name, namespace, info) with DecodeGenericType {

  def typeParameters: Seq[Option[DecodeName]] = typeParameters
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
class DecodeCommandArgumentImpl(name: DecodeName, info: Option[String], val `type`: DecodeMaybeProxy[DecodeType],
                                val unit: Option[DecodeMaybeProxy[DecodeUnit]])
  extends AbstractDecodeNameAndOptionalInfoAware(name, info) with DecodeCommandArgument

class SimpleDecodeComponent(name: DecodeName, namespace: DecodeNamespace, var id: Option[Int],
                            var baseType: Option[DecodeMaybeProxy[DecodeType]], info: Option[String],
                            var subComponents: Seq[DecodeComponentRef], var commands: Seq[DecodeCommand],
                            var messages: Seq[DecodeMessage])
  extends AbstractDecodeNameNamespaceOptionalInfoAware(name, namespace, info) with DecodeComponent