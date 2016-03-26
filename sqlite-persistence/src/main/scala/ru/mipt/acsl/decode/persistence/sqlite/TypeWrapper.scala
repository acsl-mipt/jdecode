package ru.mipt.acsl.decode.persistence.sqlite

import scala.reflect.runtime.universe._

/**
  * @author Artem Shein
  */
private[sqlite] case class TypeWrapper[F, T : TypeTag](_to: F => T, _from: T => F) {
  def t: Type = typeOf[T]
  def to(from: F): T = _to(from)
  def from(obj: T): F = _from(obj)
}
