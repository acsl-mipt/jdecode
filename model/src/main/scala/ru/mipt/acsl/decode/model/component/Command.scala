package ru.mipt.acsl.decode.model.component

import ru.mipt.acsl.decode.model._
import ru.mipt.acsl.decode.model.naming.{ElementName, HasName}
import ru.mipt.acsl.decode.model.proxy.MaybeProxy
import ru.mipt.acsl.decode.model.types.{AbstractNameInfoAware, DecodeType, Parameter, TypeMeasure}

import scala.collection.immutable

/**
  * @author Artem Shein
  */
trait Command extends HasInfo with HasName with HasOptionId {

  def parameters: immutable.Seq[Parameter]

  def returnTypeUnit: TypeMeasure

  def returnTypeProxy: MaybeProxy[DecodeType] = returnTypeUnit.typeProxy

  def returnType: DecodeType = returnTypeProxy.obj

}

object Command {

  private class CommandImpl(name: ElementName, val id: Option[Int], info: LocalizedString,
                            val parameters: immutable.Seq[Parameter], val returnTypeUnit: TypeMeasure)
    extends AbstractNameInfoAware(name, info) with Command

  def apply(name: ElementName, id: Option[Int], info: LocalizedString,
            parameters: immutable.Seq[Parameter], returnTypeUnit: TypeMeasure): Command =
    new CommandImpl(name, id, info, parameters, returnTypeUnit)
}
