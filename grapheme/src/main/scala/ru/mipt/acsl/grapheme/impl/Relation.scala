package ru.mipt.acsl.grapheme.impl

import ru.mipt.acsl.{grapheme => g}

/**
  * @author Artem Shein
  */
case class Relation(bytes: Array[Byte], relations: Map[g.Relation, Set[g.Node]] = Map.empty)
  extends g.Relation
