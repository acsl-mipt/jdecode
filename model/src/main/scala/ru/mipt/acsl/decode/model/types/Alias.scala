package ru.mipt.acsl.decode.model.types

import ru.mipt.acsl.decode.model.component.message.{EventMessage, StatusMessage}
import ru.mipt.acsl.decode.model.component.{Command, Component}
import ru.mipt.acsl.decode.model.naming.{ElementName, HasName, Namespace}
import ru.mipt.acsl.decode.model.proxy.MaybeProxy
import ru.mipt.acsl.decode.model.registry.Measure
import ru.mipt.acsl.decode.model.{types, _}

/**
  * @author Artem Shein
  */
sealed trait Alias extends HasName with Referenceable with HasInfo {

  def parent: Referenceable

  def info: LocalizedString

  def obj: Referenceable

}

object Alias {

  sealed trait Type extends Alias {

    def obj: DecodeType

  }

  // We don't want to useparent & obj in equals and hashCode
  final case class NsType(name: ElementName, info: LocalizedString)(val parent: Namespace, var obj: DecodeType)
    extends Type

  final case class ComponentCommand(name: ElementName, info: LocalizedString)(val parent: Component, var obj: Command)
    extends Alias

  final case class NsComponent(name: ElementName, info: LocalizedString)(val parent: Namespace, var obj: Component)
    extends Alias

  final case class NsMeasure(name: ElementName, info: LocalizedString)(val parent: Namespace, var obj: Measure)
    extends Alias

  final case class NsConst(name: ElementName, info: LocalizedString)
                          (val parent: Namespace, var obj: types.Const) extends Type

  final case class NsNs(name: ElementName, info: LocalizedString)(var parent: Namespace, var obj: Namespace)
    extends Alias

  final case class NsReferenceable(name: ElementName, info: LocalizedString)
                                  (val parent: Namespace, var obj: Referenceable) extends Alias

  final case class NsTypeMeasure(name: ElementName, info: LocalizedString)
                                (val parent: Namespace, var obj: TypeMeasure)
    extends Alias

  final case class ComponentComponent(name: ElementName, info: LocalizedString)
                                     (val parent: Component, var component: MaybeProxy.Component) extends Alias {

    def obj: Component = component.obj

  }

  final case class ComponentParameter(name: ElementName, info: LocalizedString)
                                     (val parent: Component, var obj: Parameter)
    extends Alias

  final case class ComponentEventMessage(name: ElementName, info: LocalizedString)
                                        (val parent: Component, var obj: EventMessage)
    extends Alias

  final case class ComponentStatusMessage(name: ElementName, info: LocalizedString)
                                         (val parent: Component, var obj: StatusMessage)
    extends Alias

  final case class EnumConstant(name: ElementName, info: LocalizedString)
                               (val parent: EnumType, var obj: types.EnumConstant)
    extends Alias

  final case class StructField(name: ElementName, info: LocalizedString)
                              (val parent: StructType, var obj: types.StructField)
    extends Alias

  final case class MessageOrCommandParameter(name: ElementName, info: LocalizedString)
                                            (val parent: CommandOrTmMessage, var obj: Parameter)
    extends Alias

}
