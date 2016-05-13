package ru.mipt.acsl.modeling

/**
  * @author Artem Shein
  */
trait TransformationResult[T] {
  def result: Option[T]
  def messages: Seq[TransformationMessage]
  def hasError: Boolean = messages.exists(_.level == ErrorLevel)
}
