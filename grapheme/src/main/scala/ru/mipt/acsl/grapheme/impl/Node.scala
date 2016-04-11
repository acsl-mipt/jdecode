package ru.mipt.acsl.grapheme.impl

import ru.mipt.acsl.grapheme

/**
  * @author Artem Shein
  */
private[grapheme] case class Node(bytes: Array[Byte], relations: Map[Relation, Set[Node]] = Map.empty)
  extends grapheme.Node
