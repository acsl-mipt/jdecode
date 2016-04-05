package ru.mipt.acsl.decode.model.domain.pure.component.message

import ru.mipt.acsl.decode.model.domain.pure.component.Component
import ru.mipt.acsl.decode.model.domain.pure.types.{DecodeType, StructField}


trait MessageParameterRef {
  def component: Component

  def structField: Option[StructField]

  def path: MessageParameterPath

  def resultType: DecodeType

  def t: DecodeType
}