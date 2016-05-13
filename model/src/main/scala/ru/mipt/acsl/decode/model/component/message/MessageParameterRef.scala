package ru.mipt.acsl.decode.model.component.message

import ru.mipt.acsl.decode.model.component.Component
import ru.mipt.acsl.decode.model.types.{DecodeType, StructField}


trait MessageParameterRef {
  def component: Component

  def structField: Option[StructField]

  def path: MessageParameterPath

  def resultType: DecodeType

  def t: DecodeType
}