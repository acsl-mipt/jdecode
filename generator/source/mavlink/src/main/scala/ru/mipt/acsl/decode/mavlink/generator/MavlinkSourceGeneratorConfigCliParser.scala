package ru.mipt.acsl.decode.mavlink.generator

import java.io.File

/**
  * @author Artem Shein
  */
class MavlinkSourceGeneratorConfigCliParser
  extends scopt.OptionParser[MavlinkSourceGeneratorConfig]("mavlink2decode") {

  head("mavlink2decode", "0.1")
  opt[String]('n', "namespace") action { case (n, c) => c.copy(nsFqn = n) }
  arg[File]("<input file>") action { case(i, c) => c.copy(input = i) }
  arg[File]("<output file>") action { case (o, c) => c.copy(output = o) }

  def parse(args: Array[String]): Option[MavlinkSourceGeneratorConfig] =
    parse(args, MavlinkSourceGeneratorConfig())

}

object MavlinkSourceGeneratorConfigCliParser {

  def apply() = new MavlinkSourceGeneratorConfigCliParser()

}
