package ru.mipt.acsl.decode.model.domain.impl.component.message

import ru.mipt.acsl.decode.model.domain.impl.component.Component
import ru.mipt.acsl.decode.model.domain.impl.types.{DecodeType, StructField}
import ru.mipt.acsl.decode.model.domain.pure
import ru.mipt.acsl.decode.model.domain.pure.component.messages.MessageParameterPath

/**
  * Created by metadeus on 18.03.16.
  */
trait MessageParameterRef extends pure.component.messages.MessageParameterRef {
  override def component: Component

  override def structField: Option[StructField]

  override def path: MessageParameterPath

  override def t: DecodeType
}
