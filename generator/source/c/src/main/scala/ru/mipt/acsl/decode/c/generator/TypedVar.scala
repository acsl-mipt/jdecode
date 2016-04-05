package ru.mipt.acsl.decode.c.generator

import ru.mipt.acsl.generator.c.ast.{CFuncParam, CStructTypeDefField, CType, CVar}

/**
  * @author Artem Shein
  */
private case class TypedVar(name: String, t: CType) {
  val v: CVar = CVar(name)
  val param: CFuncParam = CFuncParam(name, t)
  val field: CStructTypeDefField = CStructTypeDefField(name, t)
}
