package ru.mipt.acsl.decode.model.types

import ru.mipt.acsl.decode.model.naming.{ElementName, Fqn}

/**
  * @author Artem Shein
  */
class PrimitiveTypeInfo(val bitLength: Long, val kind: TypeKind.Value)

object PrimitiveTypeInfo {

  private def fqn(name: String) = Fqn.newInstance(Fqn.DECODE_NAMESPACE, ElementName.newInstanceFromSourceName(name))

  private def typeInfo(bitLength: Long, kind: TypeKind.Value): PrimitiveTypeInfo = new PrimitiveTypeInfo(bitLength, kind)

  val typeInfoByFqn = Map(
    fqn("u8") -> typeInfo(8, TypeKind.Uint),
    fqn("u16") -> typeInfo(16, TypeKind.Uint),
    fqn("u32") -> typeInfo(32, TypeKind.Uint),
    fqn("u64") -> typeInfo(64, TypeKind.Uint),
    fqn("i8") -> typeInfo(8, TypeKind.Int),
    fqn("i16") -> typeInfo(16, TypeKind.Int),
    fqn("i32") -> typeInfo(32, TypeKind.Int),
    fqn("i64") -> typeInfo(64, TypeKind.Int),
    fqn("bool") -> typeInfo(8, TypeKind.Bool),
    fqn("f32") -> typeInfo(32, TypeKind.Float),
    fqn("f64") -> typeInfo(64, TypeKind.Float)
  )
}