package ru.mipt.acsl.decode.mavlink.generator

import java.io.File

/**
  * @author Artem Shein
  */
case class MavlinkSourceGeneratorConfig(input: File = new File("in"), output: File = new File("out"),
                                        nsFqn: String = "mavlink")
