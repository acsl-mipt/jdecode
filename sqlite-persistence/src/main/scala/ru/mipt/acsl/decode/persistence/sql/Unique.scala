package ru.mipt.acsl.decode.persistence.sql

/**
  * Created by metadeus on 22.03.16.
  */
case class Unique(fields: String*) extends Constraint {
  override def toString: String = s"UNIQUE(${fields.mkString(", ")})"
}
