package ru.mipt.acsl.decode.model.component.message

import java.util
import java.util.Optional

import org.jetbrains.annotations.Nullable
import ru.mipt.acsl.decode.model.component.Component
import ru.mipt.acsl.decode.model.naming.ElementName
import ru.mipt.acsl.decode.model.proxy.{MaybeProxyCompanion, MaybeTypeProxy}
import ru.mipt.acsl.decode.model.registry.Language
import ru.mipt.acsl.decode.model.types.{Alias, DecodeType}
import ru.mipt.acsl.decode.model.{Referenceable, TmParameter}

import scala.collection.JavaConversions._

trait EventMessage extends TmMessage {

  def alias: Alias.ComponentEventMessage

  def baseTypeProxy: MaybeTypeProxy

  def baseType: DecodeType = baseTypeProxy.obj

  def name: ElementName = alias.name

  override def info: util.Map[Language, String] = alias.info

  override def component: Component = alias.parent

  def parameters: Seq[TmParameter] = objects.flatMap {
    case m: TmParameter => Seq(m)
    case _ => Seq.empty
  }

}

object EventMessage {

  private case class EventMessageImpl(alias: Alias.ComponentEventMessage, @Nullable _id: Integer,
                                      var objects: util.List[Referenceable],
                                      baseTypeProxy: MaybeTypeProxy)
    extends EventMessage {

    override def setObjects(objects: util.List[Referenceable]): Unit = this.objects = objects

    override def id(): Optional[Integer] = Optional.ofNullable(_id)
  }

  def apply(alias: Alias.ComponentEventMessage, @Nullable id: Integer,
            objects: util.List[Referenceable],
            baseTypeProxy: MaybeTypeProxy): EventMessage =
    EventMessageImpl(alias, id, objects, baseTypeProxy)
}
