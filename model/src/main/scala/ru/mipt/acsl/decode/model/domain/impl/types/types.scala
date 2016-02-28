package ru.mipt.acsl.decode.model.domain.impl.types

import ru.mipt.acsl.decode.model.domain.aliases.{ValidatingResult, MessageParameterToken}
import ru.mipt.acsl.decode.model.domain._
import ru.mipt.acsl.decode.model.domain.impl.{ElementName, ParameterParser}
import ru.mipt.acsl.decode.model.domain.proxy.aliases.ResolvingResult
import ru.mipt.acsl.decode.model.domain.proxy.{Result, MaybeProxy}

import scala.collection.immutable
import scala.collection.mutable

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

abstract class AbstractOptionalInfoAware(val info: Option[String]) extends HasOptionInfo

abstract class AbstractNameAndOptionInfoAware(val name: ElementName, info: Option[String])
  extends AbstractOptionalInfoAware(info) with NameAndOptionInfoAware {
}

class AbstractNameAndOptionalInfoAware(val name: ElementName, info: Option[String])
  extends AbstractOptionalInfoAware(info) with HasName

class AbstractNameNamespaceOptionalInfoAware(name: ElementName, var namespace: Namespace, info: Option[String])
  extends AbstractNameAndOptionalInfoAware(name, info) with NamespaceAware

class MeasureImpl(name: ElementName, var namespace: Namespace, var display: Option[String], info: Option[String])
  extends AbstractNameAndOptionalInfoAware(name, info) with Measure {
  override def validate(registry: Registry): ValidatingResult = {
    // TODO
    Result.empty
  }
}

object Namespace {
  def apply(name: ElementName, info: String = null, parent: Option[Namespace] = None,
            types: immutable.Seq[DecodeType] = immutable.Seq.empty,
            units: immutable.Seq[Measure] = immutable.Seq.empty,
            subNamespaces: immutable.Seq[Namespace] = immutable.Seq.empty,
            components: immutable.Seq[Component] = immutable.Seq.empty,
            languages: immutable.Seq[Language] = immutable.Seq.empty) =
    new NamespaceImpl(name, Option(info), parent, types, units, subNamespaces, components, languages)
}

class NamespaceImpl(var name: ElementName, var info: Option[String], var parent: Option[Namespace],
                    var types: immutable.Seq[DecodeType], var units: immutable.Seq[Measure],
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
}

// Types

abstract class AbstractType(name: ElementName, var namespace: Namespace, info: Option[String])
  extends AbstractNameAndOptionInfoAware(name, info) with DecodeType

abstract class AbstractTypeWithBaseType(name: ElementName, namespace: Namespace,
                                        info: Option[String], var baseType: MaybeProxy[DecodeType])
  extends AbstractType(name, namespace, info)

class PrimitiveTypeImpl(name: ElementName, namespace: Namespace, info: Option[String],
                        val kind: TypeKind.Value, val bitLength: Long = 0)
  extends AbstractType(name, namespace, info) with PrimitiveType

class AliasTypeImpl(val name: ElementName, var namespace: Namespace, val baseType: MaybeProxy[DecodeType],
                    info: Option[String])
  extends AbstractOptionalInfoAware(info) with AliasType with HasBaseType {
  def optionName = Some(name)
}

private class SubTypeImpl(name: ElementName, namespace: Namespace, info: Option[String],
                          val baseType: MaybeProxy[DecodeType])
  extends AbstractType(name, namespace, info) with SubType

object SubType {
  def apply(name: ElementName, namespace: Namespace, info: Option[String],
            baseType: MaybeProxy[DecodeType]): SubType = new SubTypeImpl(name, namespace, info, baseType)
}

class EnumConstantImpl(val name: ElementName, val value: String, info: Option[String])
  extends AbstractOptionalInfoAware(info) with EnumConstant

class EnumTypeImpl(name: ElementName, namespace: Namespace,
                   var extendsOrBaseType: Either[MaybeProxy[EnumType], MaybeProxy[DecodeType]],
                   info: Option[String], var constants: Set[EnumConstant], var isFinal: Boolean)
  extends AbstractType(name, namespace, info) with EnumType {
  override def extendsType: Option[MaybeProxy[EnumType]] = extendsOrBaseType.left.toOption
  def baseTypeOption: Option[MaybeProxy[DecodeType]] = extendsOrBaseType.right.toOption
  override def baseType: MaybeProxy[DecodeType] = extendsOrBaseType.right.getOrElse(extendsType.get.obj.baseType)
}

class TypeUnitImpl(val t: MaybeProxy[DecodeType], val unit: Option[MaybeProxy[Measure]])
  extends TypeUnit

class StructFieldImpl(val name: ElementName, val typeUnit: TypeUnit, info: Option[String])
  extends AbstractOptionalInfoAware(info) with StructField {
  override def toString: String = s"${this.getClass.getSimpleName}{name = $name, typeUnit = $typeUnit, info = $info}"
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
class ParameterImpl(name: ElementName, info: Option[String], val paramType: MaybeProxy[DecodeType],
                    val unit: Option[MaybeProxy[Measure]])
  extends AbstractNameAndOptionalInfoAware(name, info) with Parameter

class ComponentImpl(name: ElementName, namespace: Namespace, var id: Option[Int],
                    var baseType: Option[MaybeProxy[StructType]], info: Option[String],
                    var subComponents: immutable.Seq[ComponentRef],
                    var commands: immutable.Seq[Command] = immutable.Seq.empty,
                    var eventMessages: immutable.Seq[EventMessage] = immutable.Seq.empty,
                    var statusMessages: immutable.Seq[StatusMessage] = immutable.Seq.empty)
  extends AbstractNameNamespaceOptionalInfoAware(name, namespace, info) with Component {
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