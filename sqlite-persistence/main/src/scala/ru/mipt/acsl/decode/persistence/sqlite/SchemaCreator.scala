package ru.mipt.acsl.decode.persistence.sqlite

import com.google.common.base.CaseFormat
import ru.mipt.acsl.decode.model.domain.pure.registry.Registry
import ru.mipt.acsl.decode.persistence.sql._

import scala.collection.{GenTraversableOnce, mutable}
import scala.reflect.api.JavaUniverse
import scala.reflect.runtime.universe._

/**
  * @author Artem Shein
  */
object SchemaCreator {

  private def lowerCamelToLowerUnderscore(s: String) = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, s)
  private def upperCamelToLowerUnderscore(s: String) = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, s)

  def sqlTypeFor(m: Symbol): SqlType = {
    m.asMethod.returnType match {
      case t if t =:= typeOf[Int] => SqlInt
      case t if t <:< typeOf[Seq[_]] => SqlInt
      case t => sys.error(s"not implemented for ${m.fullName}: ${t.typeSymbol}")
    }
  }

  def seqTableName(tpe: Type, st: Type): String = tpe.tableName + "_" + st.tableName

  def constraintsFor(tpe: Type, m: Symbol): Seq[Constraint] = m.asMethod.returnType match {
    case t if t =:= typeOf[Int] => Seq.empty
    case t if t <:< typeOf[Seq[_]] => t.typeArgs.head match {
      case st =>
        val name = tpe.tableName
        Seq(ForeignKey(m.fieldName, seqTableName(tpe, st), name))
    }
    case t => sys.error(s"not implemented for ${m.fullName}: ${t.typeSymbol}")
  }

  def collectTables(m: Symbol, set: mutable.Set[Table]): Unit = m.asMethod.returnType match {
    case t if t <:< typeOf[Seq[_]] => t.typeArgs.head match {
      case st =>
        collectTables(st, set)
    }
  }

  def collectTables(t: Type, set: mutable.Set[Table]): Unit = {
    val decls = t.decls
    decls.foreach(collectTables(_, set))
    set ++= (t match {
      case _ =>
        val methods = decls
        Set(Table(t.tableName,
          methods.map(m => Field(m.fieldName, sqlTypeFor(m))).toSeq,
          methods.flatMap(m => constraintsFor(t, m)).toSeq))
    })
  }

  def tables: Seq[Table] = {
    val set = mutable.Set.empty[Table]
    collectTables(typeTag[Registry].tpe, set)
    set.toSeq
  }

  implicit class RichSymbol(val s: Symbol) {
    def fieldName: String = {
      val name = lowerCamelToLowerUnderscore(s.name.decodedName.toString)
      if (name.endsWith("s"))
        name.substring(0, name.length - 1)
      else
        name
    }
  }

  implicit class RichType(val t: Type) {
    def tableName: String = upperCamelToLowerUnderscore(t.typeSymbol.name.decodedName.toString)
    def pk: String = t match {
      case _ => "id"
    }
  }
}
