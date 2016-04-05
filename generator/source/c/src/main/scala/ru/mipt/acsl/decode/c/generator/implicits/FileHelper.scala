package ru.mipt.acsl.decode.c.generator.implicits

import java.io
import java.io.{FileOutputStream, OutputStreamWriter}

import resource._
import ru.mipt.acsl.generator.c.ast.{CComment, CGenState}
import ru.mipt.acsl.generator.c.ast.implicits._

import scala.io.Source

/**
  * @author Artem Shein
  */
private[generator] case class FileHelper(file: io.File) {
  def write(contents: CAstElements) {
    for (os <- managed(new OutputStreamWriter(new FileOutputStream(file)))) {
      contents.generate(CGenState(os))
    }
  }

  def write(source: Source): Unit = write(source.mkString)

  def write(contents: String): Unit = {
    for (os <- managed(new OutputStreamWriter(new FileOutputStream(file)))) {
      os.write(contents)
    }
  }

  def writeIfNotEmptyWithComment(contents: CAstElements, comment: String) {
    if (contents.nonEmpty)
      write(CComment(comment).eol ++ contents)
  }
}
