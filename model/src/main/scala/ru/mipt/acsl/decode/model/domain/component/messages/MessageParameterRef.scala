package ru.mipt.acsl.decode.model.domain.component.messages

import ru.mipt.acsl.decode.model.domain.aliases.MessageParameterToken
import ru.mipt.acsl.decode.model.domain.component.Component
import ru.mipt.acsl.decode.model.domain.types.{DecodeType, StructField}


trait MessageParameterRef {
  def component: Component

  def structField: Option[StructField]

  def subTokens: Seq[MessageParameterToken]

  def t: DecodeType
}