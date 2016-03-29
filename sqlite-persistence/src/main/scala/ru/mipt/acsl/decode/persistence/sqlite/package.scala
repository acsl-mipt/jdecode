package ru.mipt.acsl.decode.persistence

import com.google.common.base.CaseFormat
import ru.mipt.acsl.decode.persistence.sql._

import scala.collection.mutable

/**
  * Created by metadeus on 23.03.16.
  */
package object sqlite {

  import scala.reflect.runtime.universe._
  import SchemaCreator._

  private[sqlite] implicit class SymbolHelper(val s: Symbol) {
    def fieldName: String = {
      var name = s.name.decodedName.toString.lC2lU
      if (s.asMethod.returnType <:< typeOf[Seq[_]] && name.endsWith("s"))
        name = name.substring(0, name.length - 1)
      if (invalidFieldNames.contains(name))
        name = "_" + name
      name
    }
  }

  private[sqlite] implicit class TypesHelper(val types: Traversable[Type]) {

    def collectTables(map: mutable.Map[String, Table]): Unit =
      types.foreach(_.collectTables(map))

    def collectTypes(set: mutable.Set[Type]): Unit =
      types.foreach(_.collectTypes(set))
  }

  private[sqlite] implicit class StringHelper(val s: String) {

    import CaseFormat._

    def lC2lU: String =
      LOWER_CAMEL.to(LOWER_UNDERSCORE, s)

    def uC2lU: String =
      UPPER_CAMEL.to(LOWER_UNDERSCORE, s)
  }

  private[sqlite] implicit class TypeHelper(val t: Type) {

    def name: String = t.typeSymbol.name.decodedName.toString

    def tableName: String = name.uC2lU

    def pk: String = t match {
      case _ => "id"
    }

    def isScalaType: Boolean = t match {
      case _ if scalaTypes.contains(t) || scalaBaseTypes.exists(t <:< _) => true
      case _ => false
    }

    def pkFields: Seq[Field] = {
      Seq(Field("id", SqlInt.pk))
    }

    def baseTypesFields: Seq[Field] =
      t.baseClasses.map(t.baseType).filterNot(bt => bt.isScalaType || bt =:= t).toSet[Type].flatMap(_.fields).toSeq

    def fieldsWithPk: Seq[Field] =
      t.pkFields ++ t.fields

    def fields: Seq[Field] =
      t.baseTypesFields ++ t.decls.flatMap(t.fields(_)).toSeq

    def fields(m: Symbol): Seq[Field] = {
      val rt = m.asMethod.returnType
      if (rt.isSimple)
        Seq(Field(m.fieldName, sqlTypeFor(rt)))
      else
        Seq.empty
    }

    def constraints: Seq[Constraint] =
      t.decls.flatMap(t.constraints(_)).toSeq

    def constraints(m: Symbol): Seq[Constraint] = m.asMethod.returnType match {
      case _ if t =:= typeOf[Int] => Seq.empty
      case _ if t <:< typeOf[Seq[_]] =>
        Seq.empty
      /*val st = t.typeArgs.head
      val name = tpe.tableName
      Seq(ForeignKey(m.fieldName, seqTableName(tpe, st), name))*/
      case _ if t <:< typeOf[Option[_]] && t.typeArgs.head.isForeignKey =>
        val bt = t.typeArgs.head
        Seq(ForeignKey(m.fieldName, bt.tableName, bt.pk))
      case _ => Seq.empty
    }

    def isSimple: Boolean = t match {
      case _ if t <:< typeOf[Either[_, _]] || t <:< typeOf[Seq[_]] || t <:< typeOf[Map[_, _]] => false
      case _ => true
    }

    def refTypes: Seq[Type] = t match {
      case _ if t <:< typeOf[Option[_]] || t <:< typeOf[Seq[_]] || t <:< typeOf[Set[_]] =>
        Seq(t.typeArgs.head)
      case _ if t <:< typeOf[Map[_, _]] || t <:< typeOf[Either[_, _]] =>
        val args = t.dealias.typeArgs
        Seq(args.head, args(1))
      case _ => Seq.empty
    }

    def isForeignKey: Boolean = t match {
      case _ if t <:< typeOf[String] || t <:< typeOf[Int] || t <:< typeOf[Either[_, _]] || t <:< typeOf[Seq[_]]
        || t <:< typeOf[Map[_, _]] => false
      case _ if t <:< typeOf[Option[_]] => t.typeArgs.head.isForeignKey
      case _ => true
    }

    def collectTypes(s: Symbol, set: mutable.Set[Type]): Unit =
      s.asMethod.returnType.collectTypes(set)

    def collectTypes(set: mutable.Set[Type]): Unit = {
      if (set.contains(t))
        return
      if (t.isScalaType) {
        t.refTypes.collectTypes(set)
        return
      }
      set += t
      t.decls.foreach(d => t.collectTypes(d, set))
    }

    def collectTables(map: mutable.Map[String, Table]): Unit = {
      val name = t.tableName
      if (!map.contains(name)) {
        val decls = t.decls
        map.put(name, Table(name, t.fieldsWithPk, t.constraints))
        decls.foreach(t.collectTables(_, map))
        // process base types
        t.baseClasses.map(t.baseType).filterNot(bt => bt.isScalaType || bt =:= t || abstractTypes.contains(bt))
          .foreach(_.collectTables(map))
      }
    }

    def collectTables(m: Symbol, map: mutable.Map[String, Table]): Unit =
      m.asMethod.returnType.dealias match {
        case _ if t <:< typeOf[Seq[_]] =>
          val elType = t.typeArgs.head
          val name = seqTableName(t, elType)
          if (!map.contains(name)) {
            val classTableName = t.tableName
            val fieldTableName = elType.tableName
            val fields = Seq(Field(classTableName, SqlInt)) ++ fieldsFor(elType, m)
            val constraints = Seq(ForeignKey(classTableName, classTableName, t.pk),
              Unique(classTableName, fieldTableName)) ++ (
              if (elType.isForeignKey)
                Seq(ForeignKey(fieldTableName, fieldTableName, elType.pk))
              else
                Seq.empty)
            map.put(name, Table(name, fields, constraints))
            if (elType.isForeignKey)
              elType.refTypes.collectTables(map)
          }
        case _ if t <:< typeOf[Map[_, _]] =>
          val keyType = t.typeArgs.head
          val valType = t.typeArgs(1)
          val clsTableName = t.tableName
          val name = clsTableName + "_" + m.fieldName
          if (!map.contains(name)) {
            val keyTableName = keyType.tableName
            val fields = Seq(Field(clsTableName, SqlInt),
              Field(keyTableName, sqlTypeFor(keyType)),
              Field(valType.tableName, sqlTypeFor(valType)))
            val constraints = Seq(ForeignKey(clsTableName, clsTableName, t.pk),
              Unique(clsTableName, keyTableName))
            map.put(name, Table(name, fields, constraints))
            if (valType.isForeignKey)
              valType.collectTables(map)
          }
        case _ if t.isForeignKey => t.refTypes.collectTables(map)
        case _ if t <:< typeOf[Either[_, _]] =>
        case _ if t.isSimple =>
        case _ => t.collectTables(map)
      }
  }
}
