package ru.mipt.acsl.decode.persistence.sql

/**
  * @author Artem Shein
  */
case class SqlType(name: String, isNull: Boolean = false, isPk: Boolean = false) {
  def maybeNull: SqlType = this.copy(isNull = true)
  def pk: SqlType = this.copy(isPk = true)
  override def toString: String = name + (if (isNull) " NULL" else " NOT NULL") + (if (isPk) " PRIMARY KEY" else "")
}
