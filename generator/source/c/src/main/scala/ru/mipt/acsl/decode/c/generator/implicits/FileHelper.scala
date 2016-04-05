package ru.mipt.acsl.decode.c.generator.implicits

import java.io
import java.io.{FileOutputStream, OutputStreamWriter}

import ru.mipt.acsl.generator.c.ast.{CComment, CGenState}
import ru.mipt.acsl.generator.c.ast.implicits._

import scala.io.Source

/**
  * @author Artem Shein
  */
private[generator] case class FileHelper(file: io.File) {
  def write(contents: CAstElements) {
    val os = new OutputStreamWriter(new FileOutputStream(file))
    try {
      contents.generate(CGenState(os))
    } finally {
      os.close()
    }
  }

  def write(source: Source): Unit = write(source.mkString)

  def write(contents: String): Unit = {
    val os = new OutputStreamWriter(new FileOutputStream(file))
    try {
      os.write(contents)
    } finally {
      os.close()
    }
  }

  def writeIfNotEmptyWithComment(contents: CAstElements, comment: String) {
    if (contents.nonEmpty)
      write(CComment(comment).eol ++ contents)
  }
}
