package ru.mipt.acsl.decode.model.domain

import ru.mipt.acsl.decode.model.domain.proxy.aliases._
import ru.mipt.acsl.decode.model.domain.registry.Language

import scala.collection.immutable

package object aliases {
  type MessageParameterToken = Either[String, Int]
  type ValidatingResult = ResolvingResult
  type LocalizedString = immutable.Map[Language, String]
}