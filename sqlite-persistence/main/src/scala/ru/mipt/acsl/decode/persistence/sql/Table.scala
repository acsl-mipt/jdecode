package ru.mipt.acsl.decode.persistence.sql

/**
  * @author Artem Shein
  */
case class Table(name: String, fields: Seq[Field], constraints: Seq[Constraint])
