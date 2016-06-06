package ru.mipt.acsl.decode.model.types

import ru.mipt.acsl.decode.model.naming.{ElementName, Fqn, Namespace}
import ru.mipt.acsl.decode.model.{LocalizedString, Referenceable}

/**
  * @author Artem Shein
  */
trait DecodeType extends Referenceable {

  def namespace: Namespace

  def namespace_=(ns: Namespace): Unit

  def info: LocalizedString = alias.map(_.info).getOrElse(LocalizedString.empty)

  def alias: Option[Alias.Type]

  def systemName: String

  def typeParameters: Seq[ElementName]

  def isGeneric: Boolean = typeParameters.nonEmpty

  def nameOrSystemName: String = alias.map(_.name.asMangledString).getOrElse(systemName)

  override def toString: String = s"{alias: ${alias.toString}}"

  def fqn: Option[Fqn] = alias.map(a => Fqn(namespace.fqn.parts :+ a.name))

  def isUnit: Boolean = sys.error("not implemented")

  def isArray: Boolean = sys.error("not implemented")

  def isNative: Boolean = this match {
    case _: NativeType => true
    case _ => false
  }

  def isOrType: Boolean = fqn.contains(Fqn.Or)

  def isOptionType: Boolean = fqn.contains(Fqn.Option)

  def isVaruintType: Boolean = fqn.contains(Fqn.Varuint)

  def nameOption: Option[ElementName] = alias.map(_.name)

}