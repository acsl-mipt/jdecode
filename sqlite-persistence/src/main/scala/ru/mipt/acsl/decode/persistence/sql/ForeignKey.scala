package ru.mipt.acsl.decode.persistence.sql

/**
  * @author Artem Shein
  */
case class ForeignKey(fieldName: String, tableName: String, outerFieldName: String) extends Constraint {
  override def toString: String = s"FOREIGN KEY($fieldName) REFERENCES $tableName($outerFieldName)"
}
