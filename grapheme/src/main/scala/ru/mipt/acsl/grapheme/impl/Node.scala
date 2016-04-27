package ru.mipt.acsl.grapheme.impl

import ru.mipt.acsl.{grapheme => g}

/**
  * @author Artem Shein
  */
private[grapheme] case class Node(bytes: Array[Byte], relations: Map[g.Relation, Set[g.Node]] = Map.empty)
  extends g.Node
