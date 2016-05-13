package ru.mipt.acsl.decode

import ru.mipt.acsl.decode.model.proxy.ResolvingResult
import ru.mipt.acsl.decode.model.registry.Language

import scala.collection.immutable

package object model {

  type ValidatingResult = ResolvingResult
  type LocalizedString = immutable.Map[Language, String]

}