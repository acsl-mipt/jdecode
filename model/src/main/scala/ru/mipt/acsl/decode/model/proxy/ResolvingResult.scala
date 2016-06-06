package ru.mipt.acsl.decode.model.proxy

/**
  * Created by metadeus on 27.05.16.
  */
trait ResolvingResult[T] {

  def result: Option[T]

  def messages: ResolvingMessages

  def messages_=(messages: ResolvingMessages)

}

object ResolvingResult {

  private case class ResolvingResultImpl[T](var result: Option[T], var messages: ResolvingMessages) extends ResolvingResult[T]

  def empty[T]: ResolvingResult[T] = ResolvingResultImpl(None, ResolvingMessages.empty)

  def apply[T](result: Option[T] = None, messages: ResolvingMessages = ResolvingMessages.empty): ResolvingResult[T] =
    ResolvingResultImpl(result, messages)

}