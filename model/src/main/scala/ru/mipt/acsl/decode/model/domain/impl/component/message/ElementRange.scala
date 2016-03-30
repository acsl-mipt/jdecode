package ru.mipt.acsl.decode.model.domain.impl.component.message

import ru.mipt.acsl.decode.model.domain.pure

/**
  * Created by metadeus on 30.03.16.
  */
case class ElementRange(lowerBound: Long = 0, upperBound: Option[Long] = None)
  extends pure.component.messages.ElementRange {

  override def toString: String = "[" +
    (if (upperBound.isDefined)
      lowerBound + ".." + upperBound.get
    else if (lowerBound == 0)
      "*"
    else
      lowerBound + "..*") +
    "]"

}
