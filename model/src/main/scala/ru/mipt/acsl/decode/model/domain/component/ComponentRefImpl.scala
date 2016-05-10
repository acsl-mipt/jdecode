package ru.mipt.acsl.decode.model.domain.component

import ru.mipt.acsl.decode.model.domain.proxy.MaybeProxy

/**
  * @author Artem Shein
  */
private class ComponentRefImpl(val componentProxy: MaybeProxy[Component], val alias: Option[String] = None)
  extends ComponentRef
