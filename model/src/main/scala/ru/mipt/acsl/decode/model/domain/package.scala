package ru.mipt.acsl.decode.model

import ru.mipt.acsl.decode.model.domain.impl.proxy.ResolvingResult

import scala.collection.immutable

package object domain {
  type ValidatingResult = ResolvingResult
  type LocalizedString = immutable.Map[Language, String]
}