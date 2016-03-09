package ru.mipt.acsl.decode.model.domain.types

import ru.mipt.acsl.decode.model.domain._
import ru.mipt.acsl.decode.model.domain.aliases.ValidatingResult
import ru.mipt.acsl.decode.model.domain.naming.{Fqn, HasName}
import ru.mipt.acsl.decode.model.domain.proxy.Result
import ru.mipt.acsl.decode.model.domain.proxy.aliases._
import ru.mipt.acsl.decode.model.domain.registry.Registry

import scala.collection.mutable

trait DecodeType extends Referenceable with HasName with HasInfo with NamespaceAware with Resolvable with Validatable {

  def fqn: Fqn

  override def resolve(registry: Registry): ResolvingResult = {
    val resolvingResultList = mutable.Buffer.empty[ResolvingResult]
    this match {
      case t: EnumType =>
        resolvingResultList += (t.extendsOrBaseType match {
          case Left(extendsType) => extendsType.resolve(registry)
          case Right(baseType) => baseType.resolve(registry)
        })
      case t: HasBaseType =>
        resolvingResultList += t.baseType.resolve(registry)
      case t: StructType =>
        t.fields.foreach { f =>
          val typeUnit = f.typeUnit
          resolvingResultList += typeUnit.t.resolve(registry)
          if (typeUnit.t.isResolved)
            resolvingResultList += typeUnit.t.obj.resolve(registry)
          for (unit <- typeUnit.unit)
            resolvingResultList += unit.resolve(registry)
        }
      case t: GenericTypeSpecialized =>
        resolvingResultList += t.genericType.resolve(registry)
        t.genericTypeArguments.foreach(_.foreach(gta =>
          resolvingResultList += gta.resolve(registry)))
      case _ =>
    }
    resolvingResultList.flatten
  }
  override def validate(registry: Registry): ValidatingResult = {
    val result = mutable.Buffer.empty[ValidatingResult]
    this match {
      case t: EnumType =>
        t.extendsOrBaseType match {
          case Left(extendsType) => extendsType match {
            case e: EnumType =>
            case e =>
              result += Result.error(s"enum type can extend an enum, not a ${e.getClass}")
          }
          case Right(baseType) => baseType match {
            case _: PrimitiveType | _: NativeType =>
            case b =>
              result += Result.error(s"enum base type must be an instance of PrimitiveType or NativeType, not a ${b.getClass}")
          }
        }
      case _ =>
    }
    result.flatten
  }
}
