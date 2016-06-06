package ru.mipt.acsl.decode.model.component

import ru.mipt.acsl.decode.model._
import ru.mipt.acsl.decode.model.naming.{Container, ElementName, HasName}
import ru.mipt.acsl.decode.model.proxy.MaybeProxy
import ru.mipt.acsl.decode.model.types.{Alias, DecodeType, TypeMeasure}

/**
  * @author Artem Shein
  */
trait Command extends Referenceable with Container with HasName with MayHaveId with HasInfo {

  def alias: Alias.ComponentCommand

  def returnTypeUnit: TypeMeasure

  def returnTypeProxy: MaybeProxy.TypeProxy = returnTypeUnit.typeProxy

  def returnType: DecodeType = returnTypeProxy.obj

  def parameters: Seq[Parameter] = objects.flatMap {
    case p: Parameter => Seq(p)
    case _ => Seq.empty
  }

  override def name: ElementName = alias.name

  override def info: LocalizedString = alias.info

}

object Command {

  private case class CommandImpl(alias: Alias.ComponentCommand, id: Option[Int],
                          var objects: Seq[Referenceable], returnTypeUnit: TypeMeasure)
    extends Command

  def apply(alias: Alias.ComponentCommand, id: Option[Int],
            objects: Seq[Referenceable], returnTypeUnit: TypeMeasure): Command =
    CommandImpl(alias, id, objects, returnTypeUnit)
}
