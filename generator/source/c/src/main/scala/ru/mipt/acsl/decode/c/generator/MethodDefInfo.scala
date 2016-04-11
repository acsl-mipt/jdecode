package ru.mipt.acsl.decode.c.generator

import ru.mipt.acsl.decode.model.domain.impl.component.Component
import ru.mipt.acsl.generator.c.ast.CFuncDef

/**
  * @author Artem Shein
  */
case class MethodDefInfo(methodDef: CFuncDef, component: Component)
