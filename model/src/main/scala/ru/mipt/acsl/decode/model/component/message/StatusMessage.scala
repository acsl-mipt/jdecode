package ru.mipt.acsl.decode.model.component.message

import java.util
import java.util.Optional

import org.jetbrains.annotations.Nullable

import scala.collection.JavaConversions._
import ru.mipt.acsl.decode.model.component.{Component, StatusParameter}
import ru.mipt.acsl.decode.model.naming.ElementName
import ru.mipt.acsl.decode.model.registry.Language
import ru.mipt.acsl.decode.model.types.Alias
import ru.mipt.acsl.decode.model.{Referenceable, StatusParameter}

trait StatusMessage extends TmMessage {

  def alias: Alias.ComponentStatusMessage

  def priority: Optional[Integer]

  def parameters: util.List[StatusParameter] = objects.asInstanceOf[util.List[StatusParameter]]

  override def name: ElementName = alias.name

  override def info: util.Map[Language, String] = alias.info

}

object StatusMessage {

  private case class StatusMessageImpl(alias: Alias.ComponentStatusMessage, component: Component, @Nullable _id: Integer,
                                       var objects: util.List[Referenceable], @Nullable _priority: Integer)
    extends StatusMessage {

    override def setObjects(objects: util.List[Referenceable]): Unit = this.objects = objects

    override def id(): Optional[Integer] = Optional.ofNullable(_id)

    override def priority: Optional[Integer] = Optional.ofNullable(_priority)
  }

  def apply(alias: Alias.ComponentStatusMessage, component: Component, @Nullable id: Integer,
            parameters: Seq[StatusParameter], @Nullable priority: Integer): StatusMessage =
    StatusMessageImpl(alias, component, id, parameters, priority)
}