package ru.mipt.acsl.decode.model.domain.impl.types

/**
  * @author Artem Shein
  */
private class StructTypeImpl(name: ElementName, namespace: Namespace, info: Option[String], var fields: Seq[StructField])
  extends AbstractType(name, namespace, info) with StructType {
  override def toString: String =
    s"${this.getClass}{name = $name, namespace = $namespace, info = $info, fields = [${fields.map(_.toString).mkString(", ")}]"
}
