package ru.mipt.acsl.decode.c.generator

import ru.mipt.acsl.decode.model.domain.impl.component.Component
import ru.mipt.acsl.generator.c.ast.CFuncImpl

/**
  * @author Artem Shein
  */
case class MethodInfo(impl: CFuncImpl, component: Component, isPublic: Boolean = false)
