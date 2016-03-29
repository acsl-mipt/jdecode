package ru.mipt.acsl.decode.model.domain.pure.component.messages

import ru.mipt.acsl.decode.model.domain.pure.MessageParameterToken
import ru.mipt.acsl.decode.model.domain.pure.component.Component
import ru.mipt.acsl.decode.model.domain.pure.types.{DecodeType, StructField}


trait MessageParameterRef {
  def component: Component

  def structField: Option[StructField]

  def subTokens: Seq[MessageParameterToken]

  def t: DecodeType
}