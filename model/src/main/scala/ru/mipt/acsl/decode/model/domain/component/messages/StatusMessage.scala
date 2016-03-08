package ru.mipt.acsl.decode.model.domain.component.messages

trait StatusMessage extends TmMessage {
  def priority: Option[Int]

  def parameters: Seq[MessageParameter]
}