package ru.mipt.acsl.decode.model.domain

import ru.mipt.acsl.decode.model.domain.impl.proxy.ResolvingResult

import scala.collection.immutable

package object pure {
  type ValidatingResult = ResolvingResult
  type LocalizedString = immutable.Map[Language, String]
}