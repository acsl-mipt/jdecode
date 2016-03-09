package ru.mipt.acsl.decode.model.domain.impl.component

/**
  * @author Artem Shein
  */
private class ComponentRefImpl(val component: MaybeProxy[Component], val alias: Option[String] = None)
  extends ComponentRef
