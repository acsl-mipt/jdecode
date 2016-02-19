package ru.mipt.acsl.decode.parser

import scala.tools.refactoring.util.CompilerProvider

object ScriptParser extends CompilerProvider {
  def parse(str: String) = {
    treeFrom(str)
  }
}