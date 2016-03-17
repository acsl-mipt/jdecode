package ru.mipt.acsl.decode.persistence.sqlite

import com.google.common.base.CaseFormat
import ru.mipt.acsl.decode.model.domain.pure.registry.Registry
import ru.mipt.acsl.decode.persistence.sql.{Field, Table}

import scala.reflect.runtime.universe._

/**
  * @author Artem Shein
  */
object SchemaCreator {

  private def lowerCamelToLowerUnderscore(s: String) = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, s)
  private def upperCamelToLowerUnderscore(s: String) = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, s)

  def tables: Seq[Table] = {
    val tag = typeTag[Registry]
    Seq(Table(upperCamelToLowerUnderscore(tag.tpe.typeSymbol.name.decodedName.toString),
      tag.tpe.members.filter(_.isAbstract).map{ m =>
        Field(lowerCamelToLowerUnderscore(m.name.decodedName.toString))
      }.toSeq))
  }
}
