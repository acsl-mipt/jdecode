package ru.mipt.acsl.decode.model.domain.pure.component.message

trait StatusMessage extends TmMessage {
  def priority: Option[Int]

  def parameters: Seq[MessageParameter]
}