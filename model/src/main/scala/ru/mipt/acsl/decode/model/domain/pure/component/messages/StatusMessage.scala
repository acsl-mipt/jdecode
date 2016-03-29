package ru.mipt.acsl.decode.model.domain.pure.component.messages

trait StatusMessage extends TmMessage {
  def priority: Option[Int]

  def parameters: Seq[MessageParameter]
}