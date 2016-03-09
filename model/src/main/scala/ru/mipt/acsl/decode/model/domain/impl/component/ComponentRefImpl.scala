package ru.mipt.acsl.decode.model.domain.impl.component

import ru.mipt.acsl.decode.model.domain.component.{Component, ComponentRef}
import ru.mipt.acsl.decode.model.domain.proxy.MaybeProxy

/**
  * @author Artem Shein
  */
private class ComponentRefImpl(val component: MaybeProxy[Component], val alias: Option[String] = None)
  extends ComponentRef
