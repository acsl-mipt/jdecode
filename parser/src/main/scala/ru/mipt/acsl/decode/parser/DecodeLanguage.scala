package ru.mipt.acsl.decode.parser

import com.intellij.lang.Language

/**
 * @author Artem Shein
 */
object DecodeLanguage {
  val instance = new DecodeLanguage()
}

class DecodeLanguage extends Language("decode")
