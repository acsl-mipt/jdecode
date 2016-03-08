package ru.mipt.acsl.decode.model.domain.impl.types

import ru.mipt.acsl.decode.model.domain._
import ru.mipt.acsl.decode.model.domain.aliases.{MessageParameterToken, ValidatingResult}
import ru.mipt.acsl.decode.model.domain.component.messages.{EventMessage, MessageParameter, StatusMessage}
import ru.mipt.acsl.decode.model.domain.component.{Command, Component, ComponentRef, Parameter}
import ru.mipt.acsl.decode.model.domain.impl.{ElementName, ParameterParser}
import ru.mipt.acsl.decode.model.domain.naming.{ElementName, Fqn, HasName, Namespace}
import ru.mipt.acsl.decode.model.domain.proxy.aliases.ResolvingResult
import ru.mipt.acsl.decode.model.domain.proxy.{MaybeProxy, Result}
import ru.mipt.acsl.decode.model.domain.registry.{DecodeUnit, Language, Registry}
import ru.mipt.acsl.decode.model.domain.types._

import scala.collection.{immutable, mutable}

/**
  * @author Artem Shein
  */
private case class FqnImpl(parts: Seq[ElementName]) extends Fqn {
  def copyDropLast: Fqn = FqnImpl(parts.dropRight(1))
}

object Fqn {
  def apply(parts: Seq[ElementName]): Fqn = FqnImpl(parts)
  def newFromFqn(fqn: Fqn, last: ElementName): Fqn = FqnImpl(fqn.parts :+ last)
  def newFromSource(sourceText: String): Fqn =
    FqnImpl("\\.".r.split(sourceText).map(ElementName.newFromSourceName))
}

private[domain] abstract class AbstractOptionalInfoAware(val info: Option[String]) extends HasOptionInfo

private abstract class AbstractNameAndOptionInfoAware(val name: ElementName, info: Option[String])
  extends AbstractOptionalInfoAware(info) with NameAndOptionInfoAware {
}

private[domain] class AbstractNameAndOptionalInfoAware(val name: ElementName, info: Option[String])
  extends AbstractOptionalInfoAware(info) with HasName

private[domain] class AbstractNameNamespaceOptionalInfoAware(name: ElementName, var namespace: Namespace, info: Option[String])
  extends AbstractNameAndOptionalInfoAware(name, info) with NamespaceAware

private class DecodeUnitImpl(name: ElementName, var namespace: Namespace, var display: Option[String], info: Option[String])
  extends AbstractNameAndOptionalInfoAware(name, info) with DecodeUnit {
  override def validate(registry: Registry): ValidatingResult = {
    // TODO
    Result.empty
  }
}

object DecodeUnit {
  def apply(name: ElementName, namespace: Namespace, display: Option[String] = None,
            info: Option[String] = None): DecodeUnit =
    new DecodeUnitImpl(name, namespace, display, info)
}

object Namespace {
  def apply(name: ElementName, info: Option[String] = None, parent: Option[Namespace] = None,
            types: immutable.Seq[DecodeType] = immutable.Seq.empty,
            units: immutable.Seq[DecodeUnit] = immutable.Seq.empty,
            subNamespaces: immutable.Seq[Namespace] = immutable.Seq.empty,
            components: immutable.Seq[Component] = immutable.Seq.empty,
            languages: immutable.Seq[Language] = immutable.Seq.empty): Namespace =
    new NamespaceImpl(name, info, parent, types, units, subNamespaces, components, languages)
}

private class NamespaceImpl(var name: ElementName, var info: Option[String], var parent: Option[Namespace],
                    var types: immutable.Seq[DecodeType], var units: immutable.Seq[DecodeUnit],
                    var subNamespaces: immutable.Seq[Namespace], var components: immutable.Seq[Component],
                    var languages: immutable.Seq[Language])
  extends HasName with Namespace {
  override def asString: String = name.asMangledString

  override def resolve(registry: Registry): ResolvingResult = {
    val result = new mutable.ArrayBuffer[ResolvingResult]()
    result ++= types.map(_.resolve(registry))
    result ++= subNamespaces.map(_.resolve(registry))
    result ++= components.map(_.resolve(registry))
    result.flatten
  }

  override def validate(registry: Registry): ValidatingResult = {
    val result = new mutable.ArrayBuffer[ValidatingResult]()
    result ++= types.map(_.validate(registry))
    result ++= subNamespaces.map(_.validate(registry))
    result ++= components.map(_.validate(registry))
    result.flatten
  }

  override def fqn: Fqn = {
    val parts: scala.collection.mutable.Buffer[ElementName] = scala.collection.mutable.Buffer[ElementName]()
    var currentNamespace: Namespace = this
    while (currentNamespace.parent.isDefined) {
      parts += currentNamespace.name
      currentNamespace = currentNamespace.parent.get
    }
    parts += currentNamespace.name
    Fqn(parts.reverse)
  }
}

// Types

private abstract class AbstractType(name: ElementName, var namespace: Namespace, info: Option[String])
  extends AbstractNameAndOptionInfoAware(name, info) with DecodeType {
  def fqn: Fqn = Fqn(namespace.fqn.parts :+ name)
}

private abstract class AbstractTypeWithBaseType(name: ElementName, namespace: Namespace,
                                                info: Option[String], var baseType: MaybeProxy[DecodeType])
  extends AbstractType(name, namespace, info)

private class PrimitiveTypeImpl(name: ElementName, namespace: Namespace, info: Option[String],
                                val kind: TypeKind.Value, val bitLength: Long = 0)
  extends AbstractType(name, namespace, info) with PrimitiveType

object PrimitiveType {
  def apply(name: ElementName, namespace: Namespace, info: Option[String], kind: TypeKind.Value,
            bitLength: Long = 0): PrimitiveType = new PrimitiveTypeImpl(name, namespace, info, kind, bitLength)
}

private class NativeTypeImpl(name: ElementName, ns: Namespace, info: Option[String]) extends AbstractType(name, ns, info) with NativeType

object NativeType {
  def apply(name: ElementName, ns: Namespace, info: Option[String]): NativeType = new NativeTypeImpl(name, ns, info)
}

private class AliasTypeImpl(name: ElementName, namespace: Namespace, val baseType: MaybeProxy[DecodeType],
                            info: Option[String])
  extends AbstractType(name, namespace, info) with AliasType with HasBaseType {
  def optionName = Some(name)
}

object AliasType {
  def apply(name: ElementName, namespace: Namespace, baseType: MaybeProxy[DecodeType],
            info: Option[String]): AliasType = new AliasTypeImpl(name, namespace, baseType, info)
}

private class SubTypeImpl(name: ElementName, namespace: Namespace, info: Option[String],
                          val baseType: MaybeProxy[DecodeType])
  extends AbstractType(name, namespace, info) with SubType

object SubType {
  def apply(name: ElementName, namespace: Namespace, info: Option[String],
            baseType: MaybeProxy[DecodeType]): SubType = new SubTypeImpl(name, namespace, info, baseType)
}

private class EnumConstantImpl(val name: ElementName, val value: String, info: Option[String])
  extends AbstractOptionalInfoAware(info) with EnumConstant

object EnumConstant {
  def apply(name: ElementName, value: String, info: Option[String]): EnumConstant =
    new EnumConstantImpl(name, value, info)
}

private class EnumTypeImpl(name: ElementName, namespace: Namespace,
                           var extendsOrBaseType: Either[MaybeProxy[EnumType], MaybeProxy[DecodeType]],
                           info: Option[String], var constants: Set[EnumConstant], var isFinal: Boolean)
  extends AbstractType(name, namespace, info) with EnumType {
  override def extendsType: Option[MaybeProxy[EnumType]] = extendsOrBaseType.left.toOption
  def baseTypeOption: Option[MaybeProxy[DecodeType]] = extendsOrBaseType.right.toOption
  override def baseType: MaybeProxy[DecodeType] = extendsOrBaseType.right.getOrElse(extendsType.get.obj.baseType)
}

object EnumType {
  def apply(name: ElementName, namespace: Namespace,
            extendsOrBaseType: Either[MaybeProxy[EnumType], MaybeProxy[DecodeType]],
            info: Option[String], constants: Set[EnumConstant], isFinal: Boolean): EnumType =
    new EnumTypeImpl(name, namespace, extendsOrBaseType, info, constants, isFinal)
}

private class TypeUnitImpl(val t: MaybeProxy[DecodeType], val unit: Option[MaybeProxy[DecodeUnit]])
  extends TypeUnit

object TypeUnit {
  def apply(t: MaybeProxy[DecodeType], unit: Option[MaybeProxy[DecodeUnit]]): TypeUnit =
    new TypeUnitImpl(t, unit)
}

private class StructFieldImpl(val name: ElementName, val typeUnit: TypeUnit, info: Option[String])
  extends AbstractOptionalInfoAware(info) with StructField {
  override def toString: String = s"${this.getClass.getSimpleName}{name = $name, typeUnit = $typeUnit, info = $info}"
}

object StructField {
  def apply(name: ElementName, typeUnit: TypeUnit, info: Option[String]): StructField =
    new StructFieldImpl(name, typeUnit, info)
}

private class StructTypeImpl(name: ElementName, namespace: Namespace, info: Option[String], var fields: Seq[StructField])
  extends AbstractType(name, namespace, info) with StructType {
  override def toString: String =
    s"${this.getClass}{name = $name, namespace = $namespace, info = $info, fields = [${fields.map(_.toString).mkString(", ")}]"
}

object StructType {
  def apply(name: ElementName, namespace: Namespace, info: Option[String], fields: Seq[StructField]): StructType =
    new StructTypeImpl(name, namespace, info, fields)
}

private case class ArraySizeImpl(min: Long = 0, max: Long = 0) extends ArraySize {
  require(min >= 0)
  require(max >= 0)
}

object ArraySize {
  def apply(min: Long = 0, max: Long = 0): ArraySize = ArraySizeImpl(min, max)
}

private class ArrayTypeImpl(name: ElementName, ns: Namespace, info: Option[String],
                            val baseType: MaybeProxy[DecodeType], val size: ArraySize)
  extends AbstractType(name, ns, info) with ArrayType

object ArrayType {
  def apply(name: ElementName, ns: Namespace, info: Option[String], baseType: MaybeProxy[DecodeType],
            size: ArraySize): ArrayType = new ArrayTypeImpl(name, ns, info, baseType, size)
}

private class GenericTypeImpl(name: ElementName, ns: Namespace, info: Option[String],
                              val typeParameters: Seq[Option[ElementName]])
  extends AbstractType(name, ns, info) with GenericType

object GenericType {
  def apply(name: ElementName, ns: Namespace, info: Option[String],
            typeParameters: Seq[Option[ElementName]]): GenericType =
    new GenericTypeImpl(name, ns, info, typeParameters)
}

private class GenericTypeSpecializedImpl(name: ElementName, namespace: Namespace, info: Option[String],
                                 val genericType: MaybeProxy[GenericType], val genericTypeArguments: Seq[Option[MaybeProxy[DecodeType]]])
  extends AbstractType(name, namespace, info) with GenericTypeSpecialized

object GenericTypeSpecialized {
  def apply(name: ElementName, namespace: Namespace, info: Option[String],
            genericType: MaybeProxy[GenericType],
            genericTypeArguments: Seq[Option[MaybeProxy[DecodeType]]]): GenericTypeSpecialized =
    new GenericTypeSpecializedImpl(name, namespace, info, genericType, genericTypeArguments)
}

// Components
private class ParameterImpl(name: ElementName, info: Option[String], val paramType: MaybeProxy[DecodeType],
                            val unit: Option[MaybeProxy[DecodeUnit]])
  extends AbstractNameAndOptionalInfoAware(name, info) with Parameter

object Parameter {
  def apply(name: ElementName, info: Option[String], paramType: MaybeProxy[DecodeType],
            unit: Option[MaybeProxy[DecodeUnit]]): Parameter =
    new ParameterImpl(name, info, paramType, unit)
}

object Component {
  def apply(name: ElementName, namespace: Namespace, id: Option[Int], baseType: Option[MaybeProxy[StructType]],
            info: Option[String], subComponents: immutable.Seq[ComponentRef],
            commands: immutable.Seq[Command] = immutable.Seq.empty,
            eventMessages: immutable.Seq[EventMessage] = immutable.Seq.empty,
            statusMessages: immutable.Seq[StatusMessage] = immutable.Seq.empty): Component =
    new ComponentImpl(name, namespace, id, baseType, info, subComponents, commands, eventMessages, statusMessages)
}

private class ComponentImpl(name: ElementName, namespace: Namespace, var id: Option[Int],
                            var baseType: Option[MaybeProxy[StructType]], info: Option[String],
                            var subComponents: immutable.Seq[ComponentRef],
                            var commands: immutable.Seq[Command] = immutable.Seq.empty,
                            var eventMessages: immutable.Seq[EventMessage] = immutable.Seq.empty,
                            var statusMessages: immutable.Seq[StatusMessage] = immutable.Seq.empty)
  extends AbstractNameNamespaceOptionalInfoAware(name, namespace, info) with Component {
  override def fqn: Fqn = Fqn.newFromFqn(namespace.fqn, name)
  override def resolve(registry: Registry): ResolvingResult = {
    val result = mutable.Buffer.empty[ResolvingResult]
    baseType.map { t =>
      result += t.resolve(registry)
      if (t.isResolved)
        result += t.obj.resolve(registry)
    }
    commands.foreach { cmd =>
      cmd.returnType.foreach(rt => result += rt.resolve(registry))
      cmd.parameters.foreach { arg =>
        result += arg.paramType.resolve(registry)
        arg.unit.map(u => result += u.resolve(registry))
      }
    }
    eventMessages.foreach { e =>
      result += e.baseType.resolve(registry)
      e.fields.foreach {
        case Right(p) => result += p.paramType.resolve(registry)
        case _ =>
      }
    }
    subComponents.foreach { scr =>
      val sc = scr.component
      result += sc.resolve(registry)
      if (sc.isResolved)
        result += sc.obj.resolve(registry)
    }
    result.flatten
  }

  override def validate(registry: Registry): ValidatingResult = {
    val result = mutable.Buffer.empty[ResolvingResult]
    baseType.map { t =>
        result += t.obj.validate(registry)
    }
    commands.foreach { cmd =>
      cmd.returnType.foreach(rt => result += rt.obj.validate(registry))
      cmd.parameters.foreach { arg =>
        result += arg.paramType.obj.validate(registry)
        arg.unit.map(u => result += u.obj.validate(registry))
      }
    }
    eventMessages.foreach { e =>
      result += e.baseType.obj.validate(registry)
      e.fields.foreach {
        case Right(p) => result += p.paramType.obj.validate(registry)
        case _ =>
      }
    }
    subComponents.foreach { scr =>
      val sc = scr.component
      result += sc.obj.validate(registry)
    }
    result.flatten
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