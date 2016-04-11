package ru.mipt.acsl.grapheme.impl

import ru.mipt.acsl.grapheme

/**
  * @author Artem Shein
  */
case class Relation(bytes: Array[Byte], relations: Map[Relation, Set[Node]] = Map.empty)
  extends grapheme.Node
