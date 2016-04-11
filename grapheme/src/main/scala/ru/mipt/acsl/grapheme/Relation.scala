package ru.mipt.acsl.grapheme

/**
  * @author Artem Shein
  */
trait Relation extends Node

object Relation {
  def apply(bytes: Array[Byte]): Relation = impl.Relation(bytes)
}
