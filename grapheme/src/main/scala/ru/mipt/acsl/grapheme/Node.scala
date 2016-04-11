package ru.mipt.acsl.grapheme

/**
  * @author Artem Shein
  */
trait Node {
  def bytes: Array[Byte]
  def relations: Map[Relation, Set[Node]]
}

object Node {
  def apply(bytes: Array[Byte]): Node = impl.Node(bytes)
}