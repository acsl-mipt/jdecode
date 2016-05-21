package ru.mipt.acsl.decode.mavlink.generator

import java.io._
import java.nio.charset.StandardCharsets

import scala.io.Source

/**
  * @author Artem Shein
  */
case class MavlinkSourceGeneratorCliConfig(input: File = new File("in"), output: File = new File("out"),
                                           nsFqn: String = "mavlink") extends MavlinkSourceGeneratorConfig {

  override def inputContents: String = Source.fromFile(input).mkString

  override def writeOutput(contents: String): Unit = new FileOutputStream(output) {
    write(contents.getBytes(StandardCharsets.UTF_8))
    close()
  }

  override def componentName: String = "\\.".r.split(input.getName).head

  override def includeContents(fileName: String): String = Source.fromFile(new File(input.getParent, fileName)).mkString
}
