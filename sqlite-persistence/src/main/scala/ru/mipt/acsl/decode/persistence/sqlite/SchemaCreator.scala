package ru.mipt.acsl.decode.persistence.sqlite

import com.google.common.base.CaseFormat
import ru.mipt.acsl.decode.model.domain.Language
import ru.mipt.acsl.decode.model.domain.impl.naming.ElementName
import ru.mipt.acsl.decode.model.domain.impl.registry.Language
import ru.mipt.acsl.decode.model.domain.pure.naming.{ElementName, HasName}
import ru.mipt.acsl.decode.model.domain.pure.registry.Registry
import ru.mipt.acsl.decode.model.domain.pure.types._
import ru.mipt.acsl.decode.persistence.sql._
import ru.mipt.acsl.decode.persistence.sqlite

import scala.collection.mutable
import scala.reflect.runtime.universe._

object SchemaCreator {

  import sqlite._

  private[sqlite] val scalaTypes = Set(typeOf[String], typeOf[Any], typeOf[Object], typeOf[Boolean], typeOf[Unit],
    typeOf[Double], typeOf[Int], typeOf[Long])
  private[sqlite] val scalaBaseTypes = Set(typeOf[Seq[_]], typeOf[Set[_]], typeOf[Map[_, _]], typeOf[Either[_, _]],
    typeOf[Option[_]])

  private val traits = Set(typeOf[Registry], typeOf[ArrayType], typeOf[EnumType], typeOf[AliasType], typeOf[StructType],
    typeOf[SubType], typeOf[NativeType], typeOf[GenericTypeSpecialized])

  private val typeMap = Map(
    typeOf[Language] -> TypeWrapper[Language, String](l => l.code, s => Language(s)),
    typeOf[ElementName] -> TypeWrapper[ElementName, String](e => e.asMangledString, s => ElementName.newFromMangledName(s)))

  private[sqlite] val abstractTypes = Set(typeOf[HasName])

  // invalid field names for tables
  private[sqlite] val invalidFieldNames = Set("id")

  private val _types = mutable.Set.empty[Type]
  traits.collectTypes(_types)

  private val types = _types.toSet

  private val _tables = mutable.Map.empty[String, Table]
  types.collectTables(_tables)

  val tables = _tables.values.toSeq

  private[sqlite] def sqlTypeFor(t: Type): SqlType = t match {
    case _ if typeMap.contains(t) => sqlTypeFor(typeMap(t).t)
    case i if i =:= typeOf[Int] => SqlInt
    case s if s <:< typeOf[Seq[_]] => SqlInt
    case s if s <:< typeOf[String] => SqlText
    case o if o <:< typeOf[Option[_]] => sqlTypeFor(t.typeArgs.head).maybeNull
    case _ => SqlInt // foreign key
  }

  private[sqlite] def sqlTypeFor(m: Symbol): SqlType = {
    require(m.isMethod, s"${m.fullName} is not a method")
    sqlTypeFor(m.asMethod.returnType)
  }

  private[sqlite] def seqTableName(tpe: Type, st: Type): String = tpe.tableName + "_" + st.tableName

  private[sqlite] def fieldsFor(t: Type, m: Symbol): Seq[Field] = {
    if (t.isForeignKey)
      Seq(Field(t.tableName, SqlInt))
    else if (t <:< typeOf[Either[_, _]])
      Seq(Field(m.fieldName + "_l", sqlTypeFor(t.typeArgs.head)), Field(m.fieldName + "_r", sqlTypeFor(t.typeArgs(1))))
    else
      Seq(Field(m.fieldName, sqlTypeFor(t)))
  }


}
